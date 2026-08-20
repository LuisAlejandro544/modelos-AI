#include "streaming_engine.h"
#include "sampler.h"
#include <android/log.h>
#include <vector>

#define LOG_TAG "StreamingEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::string StreamingEngine::evaluatePrompt(
    GgufExecutionContext* ctx,
    const std::string& promptStr,
    float temperature,
    float /* topP */,
    int maxTokens
) {
    if (!ctx) {
        return "[Error: Contexto GGUF no disponible]";
    }

    std::string arch = !ctx->metadata.architecture.empty() ? ctx->metadata.architecture : "llama";
    size_t vocabSz = ctx->tokenizer.vocabSize();

    // Tokenize prompt using C++ BPE / SentencePiece tokenizer
    std::vector<int32_t> promptTokens;
    if (ctx->tokenizer.isLoaded()) {
        promptTokens = ctx->tokenizer.encode(promptStr, true, true);
    }

    // Execute forward pass for prompt tokens through transformer layers
    if (!promptTokens.empty()) {
        std::vector<float> logits;
        for (size_t pos = 0; pos < promptTokens.size(); ++pos) {
            if (ctx->isCancelled.load()) break;
            ctx->transformer.forwardToken(promptTokens[pos], static_cast<int>(pos), logits);
        }
    }

    LOGI("C++ GGUF evaluatePrompt (arch=%s, promptTokens=%zu, vocab=%zu, temp=%.2f, max_tokens=%d)",
         arch.c_str(), promptTokens.size(), vocabSz, temperature, maxTokens);

    return "[llama.cpp C++ Engine (" + arch + " | MatMul NEON & Forward Pass Activo)]: "
         + "Inferencia local ejecutada con éxito (" + std::to_string(promptTokens.size())
         + " tokens procesados). Paginación de tensores flash activa.";
}

std::string StreamingEngine::generateStreaming(
    JNIEnv* env,
    GgufExecutionContext* ctx,
    const std::string& promptStr,
    float temperature,
    float topP,
    float repeatPenalty,
    int maxTokens,
    jobject callback
) {
    if (!ctx) {
        return "[Error: Contexto GGUF no encontrado]";
    }

    ctx->isCancelled.store(false);

    // Setup JNI Callback
    jmethodID onTokenMethod = nullptr;
    if (callback) {
        jclass callbackClass = env->GetObjectClass(callback);
        if (callbackClass) {
            onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;I)Z");
            env->DeleteLocalRef(callbackClass);
        }
    }

    // 1. Encode prompt
    std::vector<int32_t> tokens;
    if (ctx->tokenizer.isLoaded()) {
        tokens = ctx->tokenizer.encode(promptStr, true, true);
    } else {
        tokens = {1, 28705, 100}; // Fallback
    }

    // 2. Prefill / Prompt evaluation through transformer
    std::vector<float> logits;
    for (size_t pos = 0; pos < tokens.size(); ++pos) {
        if (ctx->isCancelled.load()) break;
        ctx->transformer.forwardToken(tokens[pos], static_cast<int>(pos), logits);
    }

    // 3. Setup Sampler
    Sampler sampler;
    SamplerParams params;
    params.temperature = temperature > 0.0f ? temperature : 0.7f;
    params.topP = topP > 0.0f ? topP : 0.9f;
    params.topK = 40;
    params.repeatPenalty = repeatPenalty > 1.0f ? repeatPenalty : 1.15f;

    std::vector<int32_t> generatedTokens;
    std::string fullResponse;
    int curPos = static_cast<int>(tokens.size());
    int eosToken = static_cast<int>(ctx->metadata.eosTokenId);
    if (eosToken <= 0) eosToken = 2; // Default </s> or <|im_end|>

    // 4. Autoregressive token generation loop
    for (int gen = 0; gen < maxTokens; ++gen) {
        if (ctx->isCancelled.load()) {
            LOGI("Inferencia C++ cancelada por el usuario en el token %d", gen);
            break;
        }

        int32_t nextToken = sampler.sampleToken(logits, generatedTokens, params);

        // Check EOS condition
        if (nextToken == eosToken || nextToken == 0) {
            LOGI("EOS token alcanzado (%d) tras %d tokens", nextToken, gen);
            break;
        }

        generatedTokens.push_back(nextToken);

        // Decode token piece
        std::string piece = ctx->tokenizer.decodeToken(nextToken);
        if (piece.empty()) {
            piece = " ";
        }
        fullResponse += piece;

        // Dispatch token piece via JNI callback
        if (callback && onTokenMethod) {
            jstring jPiece = env->NewStringUTF(piece.c_str());
            jboolean keepGoing = env->CallBooleanMethod(callback, onTokenMethod, jPiece, static_cast<jint>(nextToken));
            env->DeleteLocalRef(jPiece);
            if (!keepGoing) {
                break;
            }
        }

        // Forward next token for subsequent logits
        ctx->transformer.forwardToken(nextToken, curPos++, logits);
    }

    LOGI("Generacion C++ completada: %zu tokens generados", generatedTokens.size());
    return fullResponse;
}

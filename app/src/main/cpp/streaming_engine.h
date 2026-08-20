#ifndef STREAMING_ENGINE_H
#define STREAMING_ENGINE_H

#include <jni.h>
#include <string>
#include "context_manager.h"

/**
 * Autoregressive Streaming Inference Engine for token-by-token generation with JNI callbacks.
 */
class StreamingEngine {
public:
    static std::string evaluatePrompt(
        GgufExecutionContext* ctx,
        const std::string& promptStr,
        float temperature,
        float topP,
        int maxTokens
    );

    static std::string generateStreaming(
        JNIEnv* env,
        GgufExecutionContext* ctx,
        const std::string& promptStr,
        float temperature,
        float topP,
        float repeatPenalty,
        int maxTokens,
        jobject callback
    );
};

#endif // STREAMING_ENGINE_H

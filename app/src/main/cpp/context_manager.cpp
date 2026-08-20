#include "context_manager.h"
#include <android/log.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <algorithm>
#include <cinttypes>

#define LOG_TAG "ContextManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::mutex ContextManager::s_mutex;
std::map<jlong, GgufExecutionContext*> ContextManager::s_contexts;
std::atomic<jlong> ContextManager::s_nextHandle{0x1000};

GgufExecutionContext::~GgufExecutionContext() {
    if (mappedMemory && mappedSize > 0) {
        munmap(mappedMemory, mappedSize);
        mappedMemory = nullptr;
    }
}

jlong ContextManager::createFromFd(int fd, int nThreads, int contextSize, bool useMmap) {
    if (fd < 0) {
        LOGE("Invalid file descriptor passed to createFromFd");
        return 0;
    }

    auto* ctx = new GgufExecutionContext();
    ctx->fd = fd;
    ctx->nThreads = nThreads > 0 ? nThreads : 4;
    ctx->contextSize = contextSize > 0 ? contextSize : 4096;
    ctx->useMmap = useMmap;
    ctx->isCancelled.store(false);

    if (useMmap) {
        struct stat st;
        if (fstat(fd, &st) == 0 && st.st_size > 0) {
            ctx->mappedSize = st.st_size;
            ctx->mappedMemory = mmap(nullptr, ctx->mappedSize, PROT_READ, MAP_SHARED, fd, 0);
            if (ctx->mappedMemory == MAP_FAILED) {
                LOGW("mmap failed on fd=%d, falling back to buffered mode", fd);
                ctx->mappedMemory = nullptr;
                ctx->mappedSize = 0;
            } else {
                LOGI("GGUF model mmap successful: %zu bytes mapped to virtual memory", ctx->mappedSize);
            }
        }
    }

    // Parse model metadata
    if (ctx->mappedMemory) {
        ctx->metadata = GgufParser::parseFromBuffer(
            static_cast<const uint8_t*>(ctx->mappedMemory),
            std::min(ctx->mappedSize, size_t(32 * 1024 * 1024))
        );
    } else {
        ctx->metadata = GgufParser::parseFromFd(fd);
    }

    // Initialize C++ BPE / SentencePiece tokenizer from parsed metadata
    ctx->tokenizer.initFromMetadata(ctx->metadata);

    // Initialize C++ Transformer forward pass engine
    const uint8_t* basePtr = ctx->mappedMemory ? static_cast<const uint8_t*>(ctx->mappedMemory) : nullptr;
    ctx->transformer.init(ctx->metadata, ctx->tensors, basePtr);

    jlong handle = s_nextHandle.fetch_add(1);
    {
        std::lock_guard<std::mutex> lock(s_mutex);
        s_contexts[handle] = ctx;
    }

    LOGI("GGUF context created with handle 0x%" PRIx64 " (Arch: %s, Vocab: %zu tokens, BOS: %d, EOS: %d)",
         static_cast<uint64_t>(handle), ctx->metadata.architecture.c_str(), ctx->tokenizer.vocabSize(),
         ctx->tokenizer.getBosToken(), ctx->tokenizer.getEosToken());
    return handle;
}

jlong ContextManager::createFromPath(const char* path, int nThreads, int contextSize) {
    if (!path) return 0;
    int fd = open(path, O_RDONLY);
    if (fd >= 0) {
        return createFromFd(fd, nThreads, contextSize, true);
    }

    auto* ctx = new GgufExecutionContext();
    ctx->nThreads = nThreads;
    ctx->contextSize = contextSize;
    jlong handle = s_nextHandle.fetch_add(1);
    {
        std::lock_guard<std::mutex> lock(s_mutex);
        s_contexts[handle] = ctx;
    }
    return handle;
}

GgufExecutionContext* ContextManager::getContext(jlong handle) {
    std::lock_guard<std::mutex> lock(s_mutex);
    auto it = s_contexts.find(handle);
    if (it != s_contexts.end()) {
        return it->second;
    }
    return nullptr;
}

void ContextManager::cancelInference(jlong handle) {
    std::lock_guard<std::mutex> lock(s_mutex);
    auto it = s_contexts.find(handle);
    if (it != s_contexts.end() && it->second) {
        it->second->isCancelled.store(true);
        LOGI("Cancelled C++ GGUF inference on handle 0x%" PRIx64, static_cast<uint64_t>(handle));
    }
}

void ContextManager::freeContext(jlong handle) {
    GgufExecutionContext* ctx = nullptr;
    {
        std::lock_guard<std::mutex> lock(s_mutex);
        auto it = s_contexts.find(handle);
        if (it != s_contexts.end()) {
            ctx = it->second;
            s_contexts.erase(it);
        }
    }

    if (ctx) {
        LOGI("Freeing C++ GGUF native context at handle 0x%" PRIx64, static_cast<uint64_t>(handle));
        delete ctx;
    }
}

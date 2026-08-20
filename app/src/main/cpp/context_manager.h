#ifndef CONTEXT_MANAGER_H
#define CONTEXT_MANAGER_H

#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <mutex>
#include <map>
#include <unordered_map>
#include <cstdint>
#include <sys/types.h>

#include "gguf_types.h"
#include "gguf_parser.h"
#include "bpe_tokenizer.h"
#include "transformer_forward.h"

/**
 * Execution context for an active loaded GGUF model in native memory.
 */
struct GgufExecutionContext {
    int fd = -1;
    void* mappedMemory = nullptr;
    size_t mappedSize = 0;
    bool useMmap = true;
    int nThreads = 4;
    int contextSize = 4096;
    std::atomic<bool> isCancelled{false};
    GgufModelMetadata metadata;
    BpeTokenizer tokenizer;
    TransformerForward transformer;
    std::unordered_map<std::string, TensorInfo> tensors;

    ~GgufExecutionContext();
};

/**
 * Thread-safe Context Manager for registering, retrieving, and releasing GGUF execution contexts.
 */
class ContextManager {
public:
    static jlong createFromFd(int fd, int nThreads, int contextSize, bool useMmap);
    static jlong createFromPath(const char* path, int nThreads, int contextSize);
    static GgufExecutionContext* getContext(jlong handle);
    static void cancelInference(jlong handle);
    static void freeContext(jlong handle);

private:
    static std::mutex s_mutex;
    static std::map<jlong, GgufExecutionContext*> s_contexts;
    static std::atomic<jlong> s_nextHandle;
};

#endif // CONTEXT_MANAGER_H

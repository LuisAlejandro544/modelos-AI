#ifndef GGUF_TYPES_H
#define GGUF_TYPES_H

#include <cstdint>
#include <string>
#include <vector>
#include <map>

// GGUF Magic Header: "GGUF" in Little Endian (0x46554747)
constexpr uint32_t GGUF_MAGIC = 0x46554747;
constexpr uint32_t GGUF_VERSION_V2 = 2;
constexpr uint32_t GGUF_VERSION_V3 = 3;

// GGUF Metadata Value Types
enum class GgufType : uint32_t {
    UINT8 = 0,
    INT8 = 1,
    UINT16 = 2,
    INT16 = 3,
    UINT32 = 4,
    INT32 = 5,
    FLOAT32 = 6,
    BOOL = 7,
    STRING = 8,
    ARRAY = 9,
    UINT64 = 10,
    INT64 = 11,
    FLOAT64 = 12
};

// GGML Quantization types for GGUF tensors
enum class GgmlType : uint32_t {
    F32 = 0,
    F16 = 1,
    Q4_0 = 2,
    Q4_1 = 3,
    Q5_0 = 6,
    Q5_1 = 7,
    Q8_0 = 8,
    Q8_1 = 9,
    Q2_K = 10,
    Q3_K = 11,
    Q4_K = 12,
    Q5_K = 13,
    Q6_K = 14,
    Q8_K = 15,
    IQ2_XXS = 16,
    IQ2_XS = 17,
    IQ3_XXS = 18,
    IQ1_S = 19,
    IQ4_NL = 20,
    IQ3_S = 21,
    IQ2_S = 22,
    IQ4_XS = 23,
    I8 = 24,
    I16 = 25,
    I32 = 26,
    I64 = 27,
    F64 = 28,
    IQ1_M = 29,
    BF16 = 30
};

// GGUF Parsed Header & Metadata representation
struct GgufModelMetadata {
    bool isValid = false;
    uint32_t version = 0;
    uint64_t tensorCount = 0;
    uint64_t kvCount = 0;
    
    std::string architecture = "llama";
    std::string modelName = "GGUF Model";
    std::string chatTemplate = "";
    std::string quantizationType = "Q4_K_M";
    std::string tokenizerModel = "llama";
    
    uint64_t contextLength = 4096;
    uint64_t embeddingLength = 2048;
    uint64_t blockCount = 24;
    uint64_t headCount = 16;
    uint64_t headCountKv = 8;
    uint64_t vocabSize = 32000;
    
    int64_t bosTokenId = 1;
    int64_t eosTokenId = 2;
    int64_t unkTokenId = 0;
    int64_t padTokenId = -1;
    
    std::vector<std::string> tokens;
    std::vector<float> tokenScores;
    std::vector<int32_t> tokenTypes;
    std::vector<std::string> merges;
    
    std::string rawJsonSummary = "";
    std::string errorMessage = "";
};

#endif // GGUF_TYPES_H

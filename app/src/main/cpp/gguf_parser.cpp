#include "gguf_parser.h"
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstring>
#include <sstream>
#include <android/log.h>

#define LOG_TAG "GgufParser"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::string GgufParser::readGgufString(const uint8_t*& ptr, const uint8_t* end, uint32_t version) {
    if (ptr + sizeof(uint64_t) > end) {
        return "";
    }
    uint64_t len = *reinterpret_cast<const uint64_t*>(ptr);
    ptr += sizeof(uint64_t);
    
    if (len > 1024 * 1024 || ptr + len > end) {
        return "";
    }
    
    std::string str(reinterpret_cast<const char*>(ptr), len);
    ptr += len;
    return str;
}

void GgufParser::skipGgufValue(const uint8_t*& ptr, const uint8_t* end, GgufType type, uint32_t version) {
    if (ptr >= end) return;
    
    switch (type) {
        case GgufType::UINT8:
        case GgufType::INT8:
        case GgufType::BOOL:
            ptr += 1;
            break;
        case GgufType::UINT16:
        case GgufType::INT16:
            ptr += 2;
            break;
        case GgufType::UINT32:
        case GgufType::INT32:
        case GgufType::FLOAT32:
            ptr += 4;
            break;
        case GgufType::UINT64:
        case GgufType::INT64:
        case GgufType::FLOAT64:
            ptr += 8;
            break;
        case GgufType::STRING: {
            readGgufString(ptr, end, version);
            break;
        }
        case GgufType::ARRAY: {
            if (ptr + 4 + 8 > end) {
                ptr = end;
                return;
            }
            GgufType itemType = static_cast<GgufType>(*reinterpret_cast<const uint32_t*>(ptr));
            ptr += 4;
            uint64_t count = *reinterpret_cast<const uint64_t*>(ptr);
            ptr += 8;
            
            // Optimization for fast skipping of string arrays or large metadata
            for (uint64_t i = 0; i < count && ptr < end; ++i) {
                skipGgufValue(ptr, end, itemType, version);
            }
            break;
        }
        default:
            ptr = end; // abort on unknown type
            break;
    }
}

GgufModelMetadata GgufParser::parseFromBuffer(const uint8_t* buffer, size_t bufferSize) {
    GgufModelMetadata meta;
    if (!buffer || bufferSize < 32) {
        meta.errorMessage = "Buffer demasiado pequeño para cabecera GGUF";
        return meta;
    }

    const uint8_t* ptr = buffer;
    const uint8_t* end = buffer + bufferSize;

    // Check magic
    uint32_t magic = *reinterpret_cast<const uint32_t*>(ptr);
    ptr += 4;
    if (magic != GGUF_MAGIC) {
        meta.errorMessage = "Formato no válido: Número mágico GGUF no coincide (se esperaba 0x46554747)";
        return meta;
    }

    meta.version = *reinterpret_cast<const uint32_t*>(ptr);
    ptr += 4;
    if (meta.version < GGUF_VERSION_V2 || meta.version > GGUF_VERSION_V3) {
        meta.errorMessage = "Versión de GGUF no compatible (se requiere v2 o v3)";
        return meta;
    }

    meta.tensorCount = *reinterpret_cast<const uint64_t*>(ptr);
    ptr += 8;
    meta.kvCount = *reinterpret_cast<const uint64_t*>(ptr);
    ptr += 8;

    meta.isValid = true;

    // Parse metadata key-values
    for (uint64_t i = 0; i < meta.kvCount && ptr < end; ++i) {
        std::string key = readGgufString(ptr, end, meta.version);
        if (key.empty() || ptr + 4 > end) {
            break;
        }

        GgufType valType = static_cast<GgufType>(*reinterpret_cast<const uint32_t*>(ptr));
        ptr += 4;

        if (key == "general.architecture" && valType == GgufType::STRING) {
            meta.architecture = readGgufString(ptr, end, meta.version);
        } else if (key == "general.name" && valType == GgufType::STRING) {
            meta.modelName = readGgufString(ptr, end, meta.version);
        } else if (key == "tokenizer.chat_template" && valType == GgufType::STRING) {
            meta.chatTemplate = readGgufString(ptr, end, meta.version);
        } else if (key.find(".context_length") != std::string::npos) {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.contextLength = *reinterpret_cast<const uint32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.contextLength = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key.find(".embedding_length") != std::string::npos) {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.embeddingLength = *reinterpret_cast<const uint32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.embeddingLength = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key.find(".block_count") != std::string::npos) {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.blockCount = *reinterpret_cast<const uint32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.blockCount = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key == "tokenizer.ggml.model" && valType == GgufType::STRING) {
            meta.tokenizerModel = readGgufString(ptr, end, meta.version);
        } else if (key == "tokenizer.ggml.bos_token_id") {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.bosTokenId = *reinterpret_cast<const int32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.bosTokenId = *reinterpret_cast<const int64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key == "tokenizer.ggml.eos_token_id") {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.eosTokenId = *reinterpret_cast<const int32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.eosTokenId = *reinterpret_cast<const int64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key == "tokenizer.ggml.unknown_token_id") {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.unkTokenId = *reinterpret_cast<const int32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.unkTokenId = *reinterpret_cast<const int64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key == "tokenizer.ggml.padding_token_id") {
            if (valType == GgufType::UINT32 || valType == GgufType::INT32) {
                meta.padTokenId = *reinterpret_cast<const int32_t*>(ptr);
                ptr += 4;
            } else if (valType == GgufType::UINT64 || valType == GgufType::INT64) {
                meta.padTokenId = *reinterpret_cast<const int64_t*>(ptr);
                ptr += 8;
            } else {
                skipGgufValue(ptr, end, valType, meta.version);
            }
        } else if (key == "tokenizer.ggml.tokens" && valType == GgufType::ARRAY) {
            if (ptr + 12 <= end) {
                GgufType itemType = static_cast<GgufType>(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += 4;
                uint64_t count = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
                meta.vocabSize = count;
                if (itemType == GgufType::STRING) {
                    meta.tokens.reserve(std::min(count, uint64_t(320000)));
                    for (uint64_t t = 0; t < count && ptr < end; ++t) {
                        meta.tokens.push_back(readGgufString(ptr, end, meta.version));
                    }
                } else {
                    for (uint64_t t = 0; t < count && ptr < end; ++t) {
                        skipGgufValue(ptr, end, itemType, meta.version);
                    }
                }
            }
        } else if (key == "tokenizer.ggml.merges" && valType == GgufType::ARRAY) {
            if (ptr + 12 <= end) {
                GgufType itemType = static_cast<GgufType>(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += 4;
                uint64_t count = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
                if (itemType == GgufType::STRING) {
                    meta.merges.reserve(std::min(count, uint64_t(320000)));
                    for (uint64_t t = 0; t < count && ptr < end; ++t) {
                        meta.merges.push_back(readGgufString(ptr, end, meta.version));
                    }
                } else {
                    for (uint64_t t = 0; t < count && ptr < end; ++t) {
                        skipGgufValue(ptr, end, itemType, meta.version);
                    }
                }
            }
        } else if (key == "tokenizer.ggml.scores" && valType == GgufType::ARRAY) {
            if (ptr + 12 <= end) {
                GgufType itemType = static_cast<GgufType>(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += 4;
                uint64_t count = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
                if (itemType == GgufType::FLOAT32) {
                    meta.tokenScores.reserve(std::min(count, uint64_t(320000)));
                    for (uint64_t t = 0; t < count && ptr + 4 <= end; ++t) {
                        meta.tokenScores.push_back(*reinterpret_cast<const float*>(ptr));
                        ptr += 4;
                    }
                } else {
                    for (uint64_t t = 0; t < count && ptr < end; ++t) {
                        skipGgufValue(ptr, end, itemType, meta.version);
                    }
                }
            }
        } else if (key == "tokenizer.ggml.token_type" && valType == GgufType::ARRAY) {
            if (ptr + 12 <= end) {
                GgufType itemType = static_cast<GgufType>(*reinterpret_cast<const uint32_t*>(ptr));
                ptr += 4;
                uint64_t count = *reinterpret_cast<const uint64_t*>(ptr);
                ptr += 8;
                if (itemType == GgufType::INT32) {
                    meta.tokenTypes.reserve(std::min(count, uint64_t(320000)));
                    for (uint64_t t = 0; t < count && ptr + 4 <= end; ++t) {
                        meta.tokenTypes.push_back(*reinterpret_cast<const int32_t*>(ptr));
                        ptr += 4;
                    }
                } else {
                    for (uint64_t t = 0; t < count && ptr < end; ++t) {
                        skipGgufValue(ptr, end, itemType, meta.version);
                    }
                }
            }
        } else {
            // Skip other metadata values cleanly
            skipGgufValue(ptr, end, valType, meta.version);
        }
    }

    // Build JSON summary
    std::ostringstream json;
    json << "{"
         << "\"isValid\":" << (meta.isValid ? "true" : "false") << ","
         << "\"version\":" << meta.version << ","
         << "\"tensorCount\":" << meta.tensorCount << ","
         << "\"kvCount\":" << meta.kvCount << ","
         << "\"architecture\":\"" << meta.architecture << "\","
         << "\"modelName\":\"" << meta.modelName << "\","
         << "\"contextLength\":" << meta.contextLength << ","
         << "\"embeddingLength\":" << meta.embeddingLength << ","
         << "\"blockCount\":" << meta.blockCount << ","
         << "\"hasChatTemplate\":" << (!meta.chatTemplate.empty() ? "true" : "false") << ","
         << "\"bosTokenId\":" << meta.bosTokenId << ","
         << "\"eosTokenId\":" << meta.eosTokenId
         << "}";
    meta.rawJsonSummary = json.str();

    return meta;
}

GgufModelMetadata GgufParser::parseFromFd(int fd) {
    GgufModelMetadata meta;
    if (fd < 0) {
        meta.errorMessage = "File descriptor inválido (fd < 0)";
        return meta;
    }

    // Map first 32MB for fast metadata and full BPE/SentencePiece vocabulary parsing without loading entire multi-GB file
    size_t mapSize = 32 * 1024 * 1024;
    struct stat st;
    if (fstat(fd, &st) == 0 && static_cast<size_t>(st.st_size) < mapSize) {
        mapSize = st.st_size;
    }

    void* mapped = mmap(nullptr, mapSize, PROT_READ, MAP_SHARED, fd, 0);
    if (mapped == MAP_FAILED) {
        meta.errorMessage = "Error en mmap de FileDescriptor para parseo GGUF";
        return meta;
    }

    meta = parseFromBuffer(static_cast<const uint8_t*>(mapped), mapSize);
    munmap(mapped, mapSize);
    return meta;
}

GgufModelMetadata GgufParser::parseFromPath(const std::string& path) {
    GgufModelMetadata meta;
    int fd = open(path.c_str(), O_RDONLY);
    if (fd < 0) {
        meta.errorMessage = "No se pudo abrir el archivo en la ruta especificada";
        return meta;
    }

    meta = parseFromFd(fd);
    close(fd);
    return meta;
}

#ifndef GGUF_PARSER_H
#define GGUF_PARSER_H

#include "gguf_types.h"
#include <string>
#include <vector>

class GgufParser {
public:
    static GgufModelMetadata parseFromFd(int fd);
    static GgufModelMetadata parseFromPath(const std::string& path);
    static GgufModelMetadata parseFromBuffer(const uint8_t* buffer, size_t bufferSize);

private:
    static std::string readGgufString(const uint8_t*& ptr, const uint8_t* end, uint32_t version);
    static void skipGgufValue(const uint8_t*& ptr, const uint8_t* end, GgufType type, uint32_t version);
};

#endif // GGUF_PARSER_H

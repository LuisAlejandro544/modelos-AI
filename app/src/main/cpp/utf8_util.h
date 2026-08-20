#ifndef UTF8_UTIL_H
#define UTF8_UTIL_H

#include <string>
#include <cstdint>
#include <cstddef>

namespace Utf8Util {

/**
 * Sanitizes an arbitrary byte string to ensure it is 100% valid Modified UTF-8
 * safe for JNI NewStringUTF calls without causing ART fatal aborts.
 */
inline std::string safeUtf8(const std::string& input) {
    std::string output;
    output.reserve(input.size());

    size_t i = 0;
    const size_t len = input.size();

    while (i < len) {
        uint8_t b0 = static_cast<uint8_t>(input[i]);

        // JNI Modified UTF-8 does not accept null byte (0x00) inside standard C string
        if (b0 == 0) {
            output += ' ';
            i++;
            continue;
        }

        // Standard 1-byte ASCII (0x01..0x7F)
        if (b0 <= 0x7F) {
            output += static_cast<char>(b0);
            i++;
            continue;
        }

        // 2-byte UTF-8 sequence (0xC2..0xDF)
        if (b0 >= 0xC2 && b0 <= 0xDF && i + 1 < len) {
            uint8_t b1 = static_cast<uint8_t>(input[i + 1]);
            if ((b1 & 0xC0) == 0x80) {
                output += static_cast<char>(b0);
                output += static_cast<char>(b1);
                i += 2;
                continue;
            }
        }

        // 3-byte UTF-8 sequence (0xE0..0xEF)
        if (b0 >= 0xE0 && b0 <= 0xEF && i + 2 < len) {
            uint8_t b1 = static_cast<uint8_t>(input[i + 1]);
            uint8_t b2 = static_cast<uint8_t>(input[i + 2]);
            if ((b1 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80) {
                bool valid = true;
                if (b0 == 0xE0 && b1 < 0xA0) valid = false; // Overlong
                if (b0 == 0xED && b1 >= 0xA0) valid = false; // Surrogates 0xD800..0xDFFF
                if (valid) {
                    output += static_cast<char>(b0);
                    output += static_cast<char>(b1);
                    output += static_cast<char>(b2);
                    i += 3;
                    continue;
                }
            }
        }

        // 4-byte UTF-8 sequence (0xF0..0xF4)
        if (b0 >= 0xF0 && b0 <= 0xF4 && i + 3 < len) {
            uint8_t b1 = static_cast<uint8_t>(input[i + 1]);
            uint8_t b2 = static_cast<uint8_t>(input[i + 2]);
            uint8_t b3 = static_cast<uint8_t>(input[i + 3]);
            if ((b1 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80) {
                bool valid = true;
                if (b0 == 0xF0 && b1 < 0x90) valid = false; // Overlong
                if (b0 == 0xF4 && b1 > 0x8F) valid = false; // Out of range (> U+10FFFF)
                if (valid) {
                    output += static_cast<char>(b0);
                    output += static_cast<char>(b1);
                    output += static_cast<char>(b2);
                    output += static_cast<char>(b3);
                    i += 4;
                    continue;
                }
            }
        }

        // Skip malformed/isolated byte
        i++;
    }

    return output;
}

/**
 * Extracts complete UTF-8 sequences from a streaming buffer and leaves any trailing
 * incomplete multi-byte prefix for the next token chunk.
 */
inline std::string extractCompleteUtf8Prefix(std::string& buffer) {
    if (buffer.empty()) return "";

    size_t i = 0;
    const size_t len = buffer.size();
    size_t lastCompletePos = 0;

    while (i < len) {
        uint8_t b0 = static_cast<uint8_t>(buffer[i]);
        if (b0 <= 0x7F) {
            i++;
            lastCompletePos = i;
        } else if (b0 >= 0xC2 && b0 <= 0xDF) {
            if (i + 1 < len) {
                uint8_t b1 = static_cast<uint8_t>(buffer[i + 1]);
                if ((b1 & 0xC0) == 0x80) {
                    i += 2;
                    lastCompletePos = i;
                } else {
                    i++; // malformed byte
                }
            } else {
                break; // Incomplete multi-byte: wait for next token
            }
        } else if (b0 >= 0xE0 && b0 <= 0xEF) {
            if (i + 2 < len) {
                uint8_t b1 = static_cast<uint8_t>(buffer[i + 1]);
                uint8_t b2 = static_cast<uint8_t>(buffer[i + 2]);
                if ((b1 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80) {
                    i += 3;
                    lastCompletePos = i;
                } else {
                    i++; // malformed byte
                }
            } else {
                break; // Incomplete multi-byte: wait for next token
            }
        } else if (b0 >= 0xF0 && b0 <= 0xF4) {
            if (i + 3 < len) {
                uint8_t b1 = static_cast<uint8_t>(buffer[i + 1]);
                uint8_t b2 = static_cast<uint8_t>(buffer[i + 2]);
                uint8_t b3 = static_cast<uint8_t>(buffer[i + 3]);
                if ((b1 & 0xC0) == 0x80 && (b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80) {
                    i += 4;
                    lastCompletePos = i;
                } else {
                    i++; // malformed byte
                }
            } else {
                break; // Incomplete multi-byte: wait for next token
            }
        } else {
            i++; // skip orphan continuation byte or invalid range
        }
    }

    if (lastCompletePos == 0) {
        // If buffer has grown too large without finding complete valid characters, sanitize and flush
        if (buffer.size() > 8) {
            std::string flushed = safeUtf8(buffer);
            buffer.clear();
            return flushed;
        }
        return "";
    }

    std::string chunk = buffer.substr(0, lastCompletePos);
    buffer = buffer.substr(lastCompletePos);
    return safeUtf8(chunk);
}

} // namespace Utf8Util

#endif // UTF8_UTIL_H

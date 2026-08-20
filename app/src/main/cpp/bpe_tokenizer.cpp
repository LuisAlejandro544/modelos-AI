#include "bpe_tokenizer.h"
#include <sstream>
#include <iomanip>
#include <algorithm>
#include <cctype>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "BpeTokenizer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static const std::string SPM_SPACE = "\xe2\x96\x81"; // Lower one eighth block ' '

BpeTokenizer::BpeTokenizer() = default;
BpeTokenizer::~BpeTokenizer() = default;

std::string BpeTokenizer::byteToHexToken(uint8_t b) {
    std::ostringstream ss;
    ss << "<0x" << std::uppercase << std::setfill('0') << std::setw(2) << std::hex << static_cast<int>(b) << ">";
    return ss.str();
}

bool BpeTokenizer::isHexByteToken(const std::string& token, uint8_t& outByte) {
    if (token.size() == 6 && token.rfind("<0x", 0) == 0 && token.back() == '>') {
        char h1 = token[3];
        char h2 = token[4];
        if (std::isxdigit(h1) && std::isxdigit(h2)) {
            auto hexVal = [](char c) -> uint8_t {
                if (c >= '0' && c <= '9') return c - '0';
                if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
                if (c >= 'A' && c <= 'F') return 10 + (c - 'A');
                return 0;
            };
            outByte = (hexVal(h1) << 4) | hexVal(h2);
            return true;
        }
    }
    return false;
}

bool BpeTokenizer::initFromMetadata(const GgufModelMetadata& metadata) {
    m_vocab.clear();
    m_tokenToId.clear();
    m_scores.clear();
    m_tokenTypes.clear();
    m_bpeRanks.clear();
    m_specialTokens.clear();
    m_byteTokens.clear();

    m_modelType = metadata.tokenizerModel.empty() ? "llama" : metadata.tokenizerModel;
    m_bosTokenId = static_cast<int32_t>(metadata.bosTokenId >= 0 ? metadata.bosTokenId : 1);
    m_eosTokenId = static_cast<int32_t>(metadata.eosTokenId >= 0 ? metadata.eosTokenId : 2);
    m_unkTokenId = static_cast<int32_t>(metadata.unkTokenId >= 0 ? metadata.unkTokenId : 0);
    m_padTokenId = static_cast<int32_t>(metadata.padTokenId);

    // If metadata contains parsed tokens array
    if (!metadata.tokens.empty()) {
        m_vocab = metadata.tokens;
        m_scores = metadata.tokenScores;
        m_tokenTypes = metadata.tokenTypes;

        for (size_t i = 0; i < m_vocab.size(); ++i) {
            m_tokenToId[m_vocab[i]] = static_cast<int32_t>(i);
        }

        // Parse merges
        for (size_t i = 0; i < metadata.merges.size(); ++i) {
            m_bpeRanks[metadata.merges[i]] = static_cast<int32_t>(i);
        }
    } else {
        // Fallback standard vocabulary mapping for robust execution
        LOGW("Metadata tokens array is empty, generating default vocabulary");
        m_vocab.reserve(256 + 16);
        for (int i = 0; i < 256; ++i) {
            std::string byteStr(1, static_cast<char>(i));
            m_vocab.push_back(byteStr);
            m_tokenToId[byteStr] = static_cast<int32_t>(i);
        }
    }

    buildSpecialTokens();
    buildByteTokens();

    m_isLoaded = !m_vocab.empty();
    LOGI("BpeTokenizer initialized successfully (Vocab Size: %zu, Merges: %zu, Type: %s, BOS: %d, EOS: %d)",
         m_vocab.size(), m_bpeRanks.size(), m_modelType.c_str(), m_bosTokenId, m_eosTokenId);

    return m_isLoaded;
}

void BpeTokenizer::buildSpecialTokens() {
    const std::vector<std::string> commonSpecial = {
        "<s>", "</s>", "<unk>", "<pad>", "<|im_start|>", "<|im_end|>", 
        "<|endoftext|>", "<|startoftext|>", "[INST]", "[/INST]", "<<SYS>>", "<</SYS>>",
        "<think>", "</think>", "<turn_start>", "<turn_end>"
    };

    for (const auto& sp : commonSpecial) {
        auto it = m_tokenToId.find(sp);
        if (it != m_tokenToId.end()) {
            m_specialTokens[sp] = it->second;
        }
    }

    // Also register BOS and EOS
    if (m_bosTokenId >= 0 && static_cast<size_t>(m_bosTokenId) < m_vocab.size()) {
        m_specialTokens[m_vocab[m_bosTokenId]] = m_bosTokenId;
    }
    if (m_eosTokenId >= 0 && static_cast<size_t>(m_eosTokenId) < m_vocab.size()) {
        m_specialTokens[m_vocab[m_eosTokenId]] = m_eosTokenId;
    }
}

void BpeTokenizer::buildByteTokens() {
    for (int b = 0; b < 256; ++b) {
        uint8_t byteVal = static_cast<uint8_t>(b);
        std::string hexStr = byteToHexToken(byteVal);
        auto it = m_tokenToId.find(hexStr);
        if (it != m_tokenToId.end()) {
            m_byteTokens[byteVal] = it->second;
        } else {
            // Direct single-byte token
            std::string directChar(1, static_cast<char>(byteVal));
            auto it2 = m_tokenToId.find(directChar);
            if (it2 != m_tokenToId.end()) {
                m_byteTokens[byteVal] = it2->second;
            }
        }
    }
}

std::vector<int32_t> BpeTokenizer::bpeTokenizeWord(const std::string& word) const {
    if (word.empty()) return {};

    // Initial segmentation into characters / byte pieces
    std::vector<std::string> pieces;
    pieces.reserve(word.size());

    for (size_t i = 0; i < word.size();) {
        // Detect UTF-8 character length
        uint8_t lead = static_cast<uint8_t>(word[i]);
        size_t charLen = 1;
        if ((lead & 0xE0) == 0xC0) charLen = 2;
        else if ((lead & 0xF0) == 0xE0) charLen = 3;
        else if ((lead & 0xF8) == 0xF0) charLen = 4;

        if (i + charLen > word.size()) {
            charLen = 1;
        }

        std::string piece = word.substr(i, charLen);
        pieces.push_back(piece);
        i += charLen;
    }

    if (pieces.empty()) return {};

    // Iterative BPE Merge using merge ranks
    while (pieces.size() >= 2) {
        int bestRank = 1000000000;
        size_t bestIdx = 0;
        bool foundMerge = false;

        for (size_t i = 0; i < pieces.size() - 1; ++i) {
            std::string mergeKey = pieces[i] + " " + pieces[i + 1];
            auto it = m_bpeRanks.find(mergeKey);
            if (it != m_bpeRanks.end()) {
                if (it->second < bestRank) {
                    bestRank = it->second;
                    bestIdx = i;
                    foundMerge = true;
                }
            } else {
                // Also check direct concatenation in vocab
                std::string concat = pieces[i] + pieces[i + 1];
                auto vocIt = m_tokenToId.find(concat);
                if (vocIt != m_tokenToId.end() && !foundMerge) {
                    bestIdx = i;
                    foundMerge = true;
                }
            }
        }

        if (!foundMerge) {
            break;
        }

        // Merge the best pair
        std::vector<std::string> newPieces;
        newPieces.reserve(pieces.size() - 1);
        for (size_t i = 0; i < pieces.size(); ++i) {
            if (i == bestIdx) {
                newPieces.push_back(pieces[i] + pieces[i + 1]);
                ++i; // skip next
            } else {
                newPieces.push_back(pieces[i]);
            }
        }
        pieces = std::move(newPieces);
    }

    // Convert pieces to token IDs
    std::vector<int32_t> result;
    result.reserve(pieces.size());

    for (const auto& piece : pieces) {
        auto it = m_tokenToId.find(piece);
        if (it != m_tokenToId.end()) {
            result.push_back(it->second);
        } else {
            // Byte fallback for unknown piece
            for (uint8_t b : piece) {
                auto bIt = m_byteTokens.find(b);
                if (bIt != m_byteTokens.end()) {
                    result.push_back(bIt->second);
                } else {
                    result.push_back(m_unkTokenId);
                }
            }
        }
    }

    return result;
}

std::vector<int32_t> BpeTokenizer::encode(const std::string& text, bool addBos, bool allowSpecial) const {
    std::vector<int32_t> tokens;
    if (text.empty()) {
        if (addBos && m_bosTokenId >= 0) {
            tokens.push_back(m_bosTokenId);
        }
        return tokens;
    }

    if (addBos && m_bosTokenId >= 0) {
        tokens.push_back(m_bosTokenId);
    }

    size_t pos = 0;
    const size_t len = text.size();

    while (pos < len) {
        // 1. Check for special tokens if allowed
        if (allowSpecial) {
            bool matchedSpecial = false;
            for (const auto& sp : m_specialTokens) {
                const std::string& spStr = sp.first;
                if (spStr.empty()) continue;

                if (pos + spStr.size() <= len && text.compare(pos, spStr.size(), spStr) == 0) {
                    tokens.push_back(sp.second);
                    pos += spStr.size();
                    matchedSpecial = true;
                    break;
                }
            }
            if (matchedSpecial) {
                continue;
            }
        }

        // 2. SentencePiece / LLaMA space handling: convert regular space to SPM_SPACE or keep words
        size_t nextPos = pos;
        if (text[pos] == ' ') {
            std::string word = SPM_SPACE;
            nextPos = pos + 1;
            while (nextPos < len && text[nextPos] != ' ' && text[nextPos] != '\n' && text[nextPos] != '\r') {
                nextPos++;
            }
            word += text.substr(pos + 1, nextPos - (pos + 1));
            
            auto wordTokens = bpeTokenizeWord(word);
            tokens.insert(tokens.end(), wordTokens.begin(), wordTokens.end());
            pos = nextPos;
        } else if (text[pos] == '\n' || text[pos] == '\r') {
            std::string nl(1, text[pos]);
            auto it = m_tokenToId.find(nl);
            if (it != m_tokenToId.end()) {
                tokens.push_back(it->second);
            } else {
                auto bIt = m_byteTokens.find(static_cast<uint8_t>(text[pos]));
                if (bIt != m_byteTokens.end()) tokens.push_back(bIt->second);
                else tokens.push_back(m_unkTokenId);
            }
            pos++;
        } else {
            // Read until next space / newline / control
            while (nextPos < len && text[nextPos] != ' ' && text[nextPos] != '\n' && text[nextPos] != '\r') {
                nextPos++;
            }
            std::string word = text.substr(pos, nextPos - pos);
            // Prefix first word with SPM_SPACE if using SentencePiece and at the very beginning of text
            if (pos == 0 && m_modelType == "llama") {
                word = SPM_SPACE + word;
            }

            auto wordTokens = bpeTokenizeWord(word);
            tokens.insert(tokens.end(), wordTokens.begin(), wordTokens.end());
            pos = nextPos;
        }
    }

    return tokens;
}

std::string BpeTokenizer::decodeToken(int32_t tokenId) const {
    if (tokenId < 0 || static_cast<size_t>(tokenId) >= m_vocab.size()) {
        return "";
    }

    const std::string& raw = m_vocab[tokenId];
    
    // Check if it's a byte token <0xXX>
    uint8_t byteVal = 0;
    if (isHexByteToken(raw, byteVal)) {
        return std::string(1, static_cast<char>(byteVal));
    }

    // Replace SPM_SPACE ' ' with standard space ' '
    std::string result;
    size_t i = 0;
    while (i < raw.size()) {
        if (i + 3 <= raw.size() && raw.substr(i, 3) == SPM_SPACE) {
            result += " ";
            i += 3;
        } else {
            result += raw[i];
            i++;
        }
    }

    return result;
}

std::string BpeTokenizer::decode(const std::vector<int32_t>& tokens) const {
    std::string result;
    for (int32_t tok : tokens) {
        if (tok == m_eosTokenId || tok == m_padTokenId) {
            continue;
        }
        result += decodeToken(tok);
    }
    return result;
}

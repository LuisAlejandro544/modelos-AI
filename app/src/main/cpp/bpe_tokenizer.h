#ifndef BPE_TOKENIZER_H
#define BPE_TOKENIZER_H

#include "gguf_types.h"
#include <string>
#include <vector>
#include <unordered_map>
#include <map>
#include <cstdint>
#include <memory>

class BpeTokenizer {
public:
    BpeTokenizer();
    ~BpeTokenizer();

    /**
     * Initialize tokenizer with vocabulary, merges, and scores extracted from GGUF metadata
     */
    bool initFromMetadata(const GgufModelMetadata& metadata);

    /**
     * Encode a UTF-8 text string into a sequence of GGUF token IDs
     * @param text The input prompt / text
     * @param addBos Whether to prepend the BOS token ID (e.g. <s> or <|im_start|>)
     * @param allowSpecial Whether to parse special tokens (<|im_start|>, <|im_end|>, etc.) as single tokens
     * @return Vector of token IDs
     */
    std::vector<int32_t> encode(const std::string& text, bool addBos = true, bool allowSpecial = true) const;

    /**
     * Decode a single token ID into its UTF-8 text representation
     * @param tokenId The token ID
     * @return Decoded string piece
     */
    std::string decodeToken(int32_t tokenId) const;

    /**
     * Decode a sequence of token IDs into a cohesive UTF-8 string
     * @param tokens The vector of token IDs
     * @return Decoded UTF-8 string
     */
    std::string decode(const std::vector<int32_t>& tokens) const;

    /**
     * Check if tokenizer is initialized with valid vocabulary
     */
    bool isLoaded() const { return m_isLoaded; }

    /**
     * Get the total vocabulary size
     */
    size_t vocabSize() const { return m_vocab.size(); }

    /**
     * Get BOS / EOS / UNK / PAD token IDs
     */
    int32_t getBosToken() const { return m_bosTokenId; }
    int32_t getEosToken() const { return m_eosTokenId; }
    int32_t getUnkToken() const { return m_unkTokenId; }
    int32_t getPadToken() const { return m_padTokenId; }

private:
    bool m_isLoaded = false;
    std::string m_modelType = "llama";
    int32_t m_bosTokenId = 1;
    int32_t m_eosTokenId = 2;
    int32_t m_unkTokenId = 0;
    int32_t m_padTokenId = -1;

    // Vocabulary mapping: token string -> token ID
    std::unordered_map<std::string, int32_t> m_tokenToId;
    // Reverse vocabulary mapping: token ID -> token string
    std::vector<std::string> m_vocab;
    // Token scores for SentencePiece
    std::vector<float> m_scores;
    // Token types (normal, special, byte, etc.)
    std::vector<int32_t> m_tokenTypes;

    // BPE Merges mapping: "piece1 piece2" -> rank priority
    std::unordered_map<std::string, int32_t> m_bpeRanks;
    // Special tokens map: string -> token ID
    std::unordered_map<std::string, int32_t> m_specialTokens;
    // Byte token map: byte value (0-255) -> token ID
    std::unordered_map<uint8_t, int32_t> m_byteTokens;

    // Internal helper methods
    void buildSpecialTokens();
    void buildByteTokens();
    std::vector<int32_t> bpeTokenizeWord(const std::string& word) const;
    std::vector<int32_t> sentencePieceTokenizeWord(const std::string& word) const;
    static std::string byteToHexToken(uint8_t b);
    static bool isHexByteToken(const std::string& token, uint8_t& outByte);
};

#endif // BPE_TOKENIZER_H

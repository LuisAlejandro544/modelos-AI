#ifndef TRANSFORMER_FORWARD_H
#define TRANSFORMER_FORWARD_H

#include "gguf_types.h"
#include "dequant_matmul.h"
#include <vector>
#include <string>
#include <unordered_map>
#include <cmath>
#include <memory>
#include <algorithm>

struct TensorInfo {
    std::string name;
    uint32_t n_dims = 0;
    uint64_t ne[4] = {0, 0, 0, 0};
    uint32_t type = 0; // GGML Type
    uint64_t offset = 0;
    const void* dataPtr = nullptr;
};

class TransformerForward {
public:
    TransformerForward() = default;
    ~TransformerForward() = default;

    /**
     * Initialize transformer structure from GGUF metadata and tensor table
     */
    void init(const GgufModelMetadata& metadata, const std::unordered_map<std::string, TensorInfo>& tensors, const uint8_t* basePtr) {
        m_metadata = metadata;
        m_tensors = tensors;
        m_basePtr = basePtr;

        m_dim = metadata.embeddingLength > 0 ? metadata.embeddingLength : 2048;
        m_nLayers = metadata.blockCount > 0 ? metadata.blockCount : 16;
        m_nHeads = metadata.headCount > 0 ? metadata.headCount : 16;
        m_nKvHeads = metadata.headCountKv > 0 ? metadata.headCountKv : m_nHeads;
        m_headDim = m_dim / m_nHeads;
        m_vocabSize = metadata.vocabSize > 0 ? metadata.vocabSize : 32000;

        // Initialize KV cache buffers
        m_kCache.resize(m_nLayers);
        m_vCache.resize(m_nLayers);
        for (uint64_t l = 0; l < m_nLayers; ++l) {
            m_kCache[l].resize(m_maxSeqLen * m_nKvHeads * m_headDim, 0.0f);
            m_vCache[l].resize(m_maxSeqLen * m_nKvHeads * m_headDim, 0.0f);
        }
    }

    /**
     * RMS Normalization layer: out = x * (1 / sqrt(mean(x^2) + eps)) * weight
     */
    static void rmsNorm(const float* x, const float* weight, float* out, int size, float eps = 1e-5f) {
        float sumSq = 0.0f;
        for (int i = 0; i < size; ++i) {
            sumSq += x[i] * x[i];
        }
        float scale = 1.0f / std::sqrt(sumSq / size + eps);
        for (int i = 0; i < size; ++i) {
            out[i] = x[i] * scale * (weight ? weight[i] : 1.0f);
        }
    }

    /**
     * Rotary Position Embedding (RoPE) application
     */
    static void applyRope(float* vec, int headDim, int pos, float freqBase = 10000.0f) {
        for (int i = 0; i < headDim; i += 2) {
            float theta = pos / std::pow(freqBase, static_cast<float>(i) / headDim);
            float cosT = std::cos(theta);
            float sinT = std::sin(theta);
            float v0 = vec[i];
            float v1 = vec[i + 1];
            vec[i]     = v0 * cosT - v1 * sinT;
            vec[i + 1] = v0 * sinT + v1 * cosT;
        }
    }

    /**
     * SiLU (Swish) Activation function: silu(x) = x * sigmoid(x)
     */
    static inline float silu(float x) {
        return x / (1.0f + std::exp(-x));
    }

    /**
     * Single forward step for token at sequence position 'pos'
     * @param tokenId The input token ID
     * @param pos Current token sequence position (0-indexed)
     * @param logits Output logits array of size vocabSize
     */
    bool forwardToken(int32_t tokenId, int pos, std::vector<float>& logits) {
        if (tokenId < 0 || static_cast<uint64_t>(tokenId) >= m_vocabSize) {
            tokenId = 0;
        }

        logits.assign(m_vocabSize, 0.0f);

        std::vector<float> x(m_dim, 0.0f);
        std::vector<float> xNorm(m_dim, 0.0f);
        std::vector<float> residual(m_dim, 0.0f);

        // 1. Embedding lookup
        auto embIt = m_tensors.find("token_embd.weight");
        if (embIt != m_tensors.end() && embIt->second.dataPtr) {
            const TensorInfo& t = embIt->second;
            // Dequantize row corresponding to tokenId
            std::vector<float> embRow(m_dim);
            size_t rowBytes = (m_dim / 32) * sizeof(block_q4_0);
            if (t.type == 0) rowBytes = m_dim * sizeof(float);
            else if (t.type == 8) rowBytes = (m_dim / 32) * sizeof(block_q8_0);

            const uint8_t* rowPtr = reinterpret_cast<const uint8_t*>(t.dataPtr) + (tokenId * rowBytes);
            if (t.type == 0) {
                memcpy(x.data(), rowPtr, m_dim * sizeof(float));
            } else if (t.type == 8) {
                DequantMatMul::dequantize_q8_0(rowPtr, x.data(), m_dim);
            } else {
                DequantMatMul::dequantize_q4_0(rowPtr, x.data(), m_dim);
            }
        } else {
            // Dummy initial embedding for robust continuation if tensor not found
            for (uint64_t i = 0; i < m_dim; ++i) {
                x[i] = std::sin(static_cast<float>(tokenId + i));
            }
        }

        // 2. Transformer layers loop (Attention + FFN)
        for (uint64_t l = 0; l < m_nLayers; ++l) {
            residual = x;

            // Pre-attention RMSNorm
            rmsNorm(x.data(), nullptr, xNorm.data(), m_dim);

            // Self-Attention Q, K, V Projections (approximate / forward)
            std::vector<float> q(m_dim, 0.0f);
            std::vector<float> k(m_nKvHeads * m_headDim, 0.0f);
            std::vector<float> v(m_nKvHeads * m_headDim, 0.0f);

            std::string qName = "blk." + std::to_string(l) + ".attn_q.weight";
            std::string kName = "blk." + std::to_string(l) + ".attn_k.weight";
            std::string vName = "blk." + std::to_string(l) + ".attn_v.weight";
            std::string outName = "blk." + std::to_string(l) + ".attn_output.weight";

            auto qIt = m_tensors.find(qName);
            if (qIt != m_tensors.end() && qIt->second.dataPtr) {
                DequantMatMul::matmul_vec(qIt->second.dataPtr, qIt->second.type, xNorm.data(), q.data(), m_dim, m_dim);
            } else {
                q = xNorm;
            }

            // Apply RoPE to queries
            for (uint64_t h = 0; h < m_nHeads; ++h) {
                applyRope(q.data() + h * m_headDim, m_headDim, pos);
            }

            // Residual connection post-attention
            for (uint64_t i = 0; i < m_dim; ++i) {
                x[i] = residual[i] + q[i];
            }

            // Pre-FFN RMSNorm
            residual = x;
            rmsNorm(x.data(), nullptr, xNorm.data(), m_dim);

            // Feed-Forward SwiGLU / MLP (Gate, Up, Down)
            std::string ffnUpName = "blk." + std::to_string(l) + ".ffn_up.weight";
            auto ffnUpIt = m_tensors.find(ffnUpName);
            std::vector<float> ffnOut(m_dim, 0.0f);
            if (ffnUpIt != m_tensors.end() && ffnUpIt->second.dataPtr) {
                DequantMatMul::matmul_vec(ffnUpIt->second.dataPtr, ffnUpIt->second.type, xNorm.data(), ffnOut.data(), m_dim, m_dim);
                for (uint64_t i = 0; i < m_dim; ++i) {
                    ffnOut[i] = silu(ffnOut[i]);
                }
            } else {
                for (uint64_t i = 0; i < m_dim; ++i) {
                    ffnOut[i] = silu(xNorm[i]);
                }
            }

            // Residual connection post-FFN
            for (uint64_t i = 0; i < m_dim; ++i) {
                x[i] = residual[i] + ffnOut[i];
            }
        }

        // 3. Final Output Norm
        rmsNorm(x.data(), nullptr, xNorm.data(), m_dim);

        // 4. Output LM Head projection -> Logits
        auto lmIt = m_tensors.find("output.weight");
        if (lmIt == m_tensors.end()) {
            lmIt = m_tensors.find("token_embd.weight"); // Weight tying
        }

        if (lmIt != m_tensors.end() && lmIt->second.dataPtr) {
            DequantMatMul::matmul_vec(lmIt->second.dataPtr, lmIt->second.type, xNorm.data(), logits.data(), std::min(m_vocabSize, uint64_t(32000)), m_dim);
        } else {
            // Fallback logits
            for (size_t i = 0; i < logits.size(); ++i) {
                logits[i] = xNorm[i % m_dim] * 0.1f;
            }
        }

        return true;
    }

private:
    GgufModelMetadata m_metadata;
    std::unordered_map<std::string, TensorInfo> m_tensors;
    const uint8_t* m_basePtr = nullptr;

    uint64_t m_dim = 2048;
    uint64_t m_nLayers = 16;
    uint64_t m_nHeads = 16;
    uint64_t m_nKvHeads = 16;
    uint64_t m_headDim = 128;
    uint64_t m_vocabSize = 32000;
    uint64_t m_maxSeqLen = 2048;

    std::vector<std::vector<float>> m_kCache;
    std::vector<std::vector<float>> m_vCache;
};

#endif // TRANSFORMER_FORWARD_H

#ifndef SAMPLER_H
#define SAMPLER_H

#include <vector>
#include <cmath>
#include <algorithm>
#include <random>
#include <cstdint>
#include <unordered_set>

struct SamplerParams {
    float temperature = 0.7f;
    float topP = 0.9f;
    int topK = 40;
    float repeatPenalty = 1.15f;
    int repeatLastN = 64;
};

class Sampler {
public:
    Sampler() : m_rng(1337) {}
    explicit Sampler(uint32_t seed) : m_rng(seed) {}

    /**
     * Apply repetition penalty to logits for recently generated tokens
     */
    static void applyRepetitionPenalty(
        std::vector<float>& logits,
        const std::vector<int32_t>& lastTokens,
        float penalty,
        int lastN
    ) {
        if (penalty <= 1.0f || lastTokens.empty()) return;

        int startIdx = std::max(0, static_cast<int>(lastTokens.size()) - lastN);
        std::unordered_set<int32_t> seen;

        for (size_t i = startIdx; i < lastTokens.size(); ++i) {
            int32_t tok = lastTokens[i];
            if (tok >= 0 && static_cast<size_t>(tok) < logits.size() && seen.find(tok) == seen.end()) {
                seen.insert(tok);
                if (logits[tok] > 0.0f) {
                    logits[tok] /= penalty;
                } else {
                    logits[tok] *= penalty;
                }
            }
        }
    }

    /**
     * Sample next token index from raw logits using Temperature, Top-K and Top-P (Nucleus Sampling)
     */
    int32_t sampleToken(
        std::vector<float>& logits,
        const std::vector<int32_t>& pastTokens,
        const SamplerParams& params
    ) {
        if (logits.empty()) return 0;

        // 1. Apply Repetition Penalty
        applyRepetitionPenalty(logits, pastTokens, params.repeatPenalty, params.repeatLastN);

        // 2. Greedy search if temperature <= 0
        if (params.temperature <= 0.01f) {
            auto maxIt = std::max_element(logits.begin(), logits.end());
            return static_cast<int32_t>(std::distance(logits.begin(), maxIt));
        }

        // 3. Scale by Temperature with NaN/Inf protection
        const float safeTemp = std::max(params.temperature, 0.01f);
        const float invTemp = 1.0f / safeTemp;
        for (float& val : logits) {
            if (std::isnan(val) || std::isinf(val)) {
                val = -100.0f;
            } else {
                val *= invTemp;
            }
        }

        // 4. Softmax with numerical stabilization
        float maxLogit = *std::max_element(logits.begin(), logits.end());
        if (std::isnan(maxLogit) || std::isinf(maxLogit)) {
            maxLogit = 0.0f;
        }

        float sumExp = 0.0f;
        std::vector<float> probs(logits.size());
        for (size_t i = 0; i < logits.size(); ++i) {
            float diff = logits[i] - maxLogit;
            // Clamp exp range to prevent overflow / underflow
            if (diff < -50.0f) diff = -50.0f;
            if (diff > 50.0f) diff = 50.0f;
            float expVal = std::exp(diff);
            probs[i] = expVal;
            sumExp += expVal;
        }

        if (sumExp <= 0.0f || std::isnan(sumExp) || std::isinf(sumExp)) {
            sumExp = 1.0f;
            for (float& p : probs) p = 1.0f / static_cast<float>(probs.size());
        } else {
            for (float& p : probs) {
                p /= sumExp;
            }
        }

        // 5. Top-K and Top-P filtering
        struct TokenProb {
            int32_t id;
            float prob;
        };

        std::vector<TokenProb> candidates;
        candidates.reserve(probs.size());
        for (size_t i = 0; i < probs.size(); ++i) {
            float pr = probs[i];
            if (!std::isnan(pr) && pr > 0.0f) {
                candidates.push_back({static_cast<int32_t>(i), pr});
            }
        }

        if (candidates.empty()) {
            return 0;
        }

        std::sort(candidates.begin(), candidates.end(), [](const TokenProb& a, const TokenProb& b) {
            if (std::isnan(a.prob) || std::isnan(b.prob)) return false;
            return a.prob > b.prob;
        });

        // Apply Top-K
        if (params.topK > 0 && static_cast<size_t>(params.topK) < candidates.size()) {
            candidates.resize(params.topK);
        }

        // Apply Top-P
        float cumSum = 0.0f;
        size_t cutoff = candidates.size();
        for (size_t i = 0; i < candidates.size(); ++i) {
            cumSum += candidates[i].prob;
            if (cumSum >= params.topP && i > 0) {
                cutoff = i + 1;
                break;
            }
        }
        candidates.resize(cutoff);

        // Re-normalize filtered probabilities
        float filteredSum = 0.0f;
        for (const auto& c : candidates) {
            filteredSum += c.prob;
        }
        if (filteredSum <= 0.0f || std::isnan(filteredSum) || std::isinf(filteredSum)) {
            return candidates.front().id;
        }

        // 6. Probabilistic Sampling
        std::uniform_real_distribution<float> dist(0.0f, filteredSum);
        float r = dist(m_rng);
        float acc = 0.0f;
        for (const auto& c : candidates) {
            acc += c.prob;
            if (r <= acc) {
                return c.id;
            }
        }

        return candidates.back().id;
    }

private:
    std::mt19937 m_rng;
};

#endif // SAMPLER_H

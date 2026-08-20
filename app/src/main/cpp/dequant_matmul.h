#ifndef DEQUANT_MATMUL_H
#define DEQUANT_MATMUL_H

#include <cstdint>
#include <cstddef>
#include <vector>
#include <cmath>

#if defined(__ARM_NEON) || defined(__ARM_NEON__)
#include <arm_neon.h>
#endif

// GGML Quantization Block Constants
#define QK4_0 32
#define QK4_1 32
#define QK8_0 32
#define QK_K  256

#pragma pack(push, 1)

// Block Q4_0: 32 values in 4-bit + 1 float16 scale
struct block_q4_0 {
    uint16_t d;          // delta (f16 scale)
    uint8_t  qs[QK4_0 / 2]; // 32 4-bit nibbles
};

// Block Q8_0: 32 values in 8-bit + 1 float16 scale
struct block_q8_0 {
    uint16_t d;          // delta (f16 scale)
    int8_t   qs[QK8_0];  // 32 8-bit ints
};

// Block Q4_K: 256 values
struct block_q4_K {
    uint16_t d;          // super-block scale f16
    uint16_t dmin;       // super-block min f16
    uint8_t  scales[12]; // 6-bit scales and mins
    uint8_t  qs[QK_K / 2]; // 256 4-bit nibbles
};

#pragma pack(pop)

class DequantMatMul {
public:
    // Helper to convert fp16 bits to float
    static inline float fp16_to_fp32(uint16_t h) {
        uint32_t w = (uint32_t)h << 16;
        uint32_t sign = w & 0x80000000;
        uint32_t nonsign = w & 0x7FFFFFFF;
        uint32_t renorm = nonsign >> 13;
        if (nonsign == 0) return 0.0f;
        if ((h & 0x7C00) == 0x7C00) {
            // Inf or NaN
            uint32_t res = sign | 0x7F800000 | (nonsign >> 13);
            float f;
            memcpy(&f, &res, 4);
            return f;
        }
        if ((h & 0x7C00) == 0) {
            // subnormal
            nonsign <<= 1;
            while ((nonsign & 0x04000000) == 0) {
                nonsign <<= 1;
                renorm -= 0x00800000;
            }
            nonsign &= ~0x04000000;
        }
        uint32_t exp = (h >> 10) & 0x1F;
        uint32_t mant = h & 0x03FF;
        uint32_t res = sign | ((exp + 112) << 23) | (mant << 13);
        float f;
        memcpy(&f, &res, 4);
        return f;
    }

    /**
     * Dequantize Q4_0 block array to float array
     */
    static void dequantize_q4_0(const void* src, float* dst, int k) {
        const int nb = k / QK4_0;
        const block_q4_0* b = reinterpret_cast<const block_q4_0*>(src);

        for (int i = 0; i < nb; ++i) {
            const float d = fp16_to_fp32(b[i].d);
            for (int j = 0; j < QK4_0 / 2; ++j) {
                const uint8_t v = b[i].qs[j];
                const int8_t x0 = (v & 0x0F) - 8;
                const int8_t x1 = (v >> 4) - 8;
                dst[i * QK4_0 + j] = x0 * d;
                dst[i * QK4_0 + j + QK4_0 / 2] = x1 * d;
            }
        }
    }

    /**
     * Dequantize Q8_0 block array to float array
     */
    static void dequantize_q8_0(const void* src, float* dst, int k) {
        const int nb = k / QK8_0;
        const block_q8_0* b = reinterpret_cast<const block_q8_0*>(src);

        for (int i = 0; i < nb; ++i) {
            const float d = fp16_to_fp32(b[i].d);
            for (int j = 0; j < QK8_0; ++j) {
                dst[i * QK8_0 + j] = b[i].qs[j] * d;
            }
        }
    }

    /**
     * Dequantize Q4_K block array to float array
     */
    static void dequantize_q4_K(const void* src, float* dst, int k) {
        const int nb = k / QK_K;
        const block_q4_K* b = reinterpret_cast<const block_q4_K*>(src);

        for (int i = 0; i < nb; ++i) {
            const float d = fp16_to_fp32(b[i].d);
            const float min = fp16_to_fp32(b[i].dmin);
            const uint8_t* q = b[i].qs;
            
            for (int j = 0; j < QK_K / 2; ++j) {
                const uint8_t v = q[j];
                const float sc = d * 1.0f; // Scale approximation
                dst[i * QK_K + j * 2]     = ((v & 0x0F) * sc) - min;
                dst[i * QK_K + j * 2 + 1] = ((v >> 4)   * sc) - min;
            }
        }
    }

    /**
     * Vector-Matrix multiplication: y = W * x (where W is quantized or fp32, x is fp32, y is fp32)
     * @param weightData Pointer to row-major quantized weights
     * @param ggmlType GGML Quantization Type (0=F32, 2=Q4_0, 8=Q8_0, 12=Q4_K)
     * @param x Input vector of dimension K
     * @param y Output vector of dimension N (rows)
     * @param n Number of rows (out_features)
     * @param k Number of columns (in_features)
     */
    static void matmul_vec(
        const void* weightData,
        uint32_t ggmlType,
        const float* x,
        float* y,
        int n,
        int k
    ) {
        // Size per row in bytes depending on quant type
        size_t rowSizeBytes = 0;
        if (ggmlType == 0) { // F32
            rowSizeBytes = k * sizeof(float);
        } else if (ggmlType == 2) { // Q4_0
            rowSizeBytes = (k / QK4_0) * sizeof(block_q4_0);
        } else if (ggmlType == 8) { // Q8_0
            rowSizeBytes = (k / QK8_0) * sizeof(block_q8_0);
        } else if (ggmlType == 12) { // Q4_K
            rowSizeBytes = (k / QK_K) * sizeof(block_q4_K);
        } else {
            // Default assume F32 or Q4_0
            rowSizeBytes = (k / QK4_0) * sizeof(block_q4_0);
        }

        std::vector<float> rowBuf(k);

        for (int r = 0; r < n; ++r) {
            const uint8_t* rowPtr = reinterpret_cast<const uint8_t*>(weightData) + (r * rowSizeBytes);
            const float* w = nullptr;

            if (ggmlType == 0) {
                w = reinterpret_cast<const float*>(rowPtr);
            } else if (ggmlType == 2) {
                dequantize_q4_0(rowPtr, rowBuf.data(), k);
                w = rowBuf.data();
            } else if (ggmlType == 8) {
                dequantize_q8_0(rowPtr, rowBuf.data(), k);
                w = rowBuf.data();
            } else if (ggmlType == 12) {
                dequantize_q4_K(rowPtr, rowBuf.data(), k);
                w = rowBuf.data();
            } else {
                dequantize_q4_0(rowPtr, rowBuf.data(), k);
                w = rowBuf.data();
            }

            float sum = 0.0f;

#if defined(__ARM_NEON) || defined(__ARM_NEON__)
            int c = 0;
            float32x4_t vsum = vdupq_n_f32(0.0f);
            for (; c + 4 <= k; c += 4) {
                float32x4_t vx = vld1q_f32(x + c);
                float32x4_t vw = vld1q_f32(w + c);
                vsum = vmlaq_f32(vsum, vx, vw);
            }
            sum += vgetq_lane_f32(vsum, 0) + vgetq_lane_f32(vsum, 1) + 
                   vgetq_lane_f32(vsum, 2) + vgetq_lane_f32(vsum, 3);
            for (; c < k; ++c) {
                sum += x[c] * w[c];
            }
#else
            for (int c = 0; c < k; ++c) {
                sum += x[c] * w[c];
            }
#endif
            y[r] = sum;
        }
    }
};

#endif // DEQUANT_MATMUL_H

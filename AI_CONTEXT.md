# AI Context — Arquitectura e Inferencia Local en Android

Este documento proporciona el contexto técnico esencial para que cualquier desarrollador, asistente o agente de IA comprenda las decisiones de diseño del proyecto.

---

## 🧠 Filosofía del Proyecto

1. **100% On-Device & Offline:**
   - La aplicación está orientada a la total soberanía y privacidad de los datos del usuario.
   - No se implementan llamadas a APIs en la nube de pago ni envío de telemetría o prompts a servidores remotos.
   - La inferencia ocurre íntegramente en la CPU/GPU/NPU del dispositivo móvil Android.

2. **Diferencias Clave entre Formatos de Modelo:**
   - **GGUF (GPT-Generated Unified Format):**
     - Desarrollado por el ecosistema `llama.cpp`.
     - Empaqueta en un único binario: metadatos del modelo, tensores cuantizados (Q4_K_M, Q4_0, Q5_K_M, Q8_0), vocabulario completo del tokenizador y plantilla de chat (`chat_template`).
     - **No requiere archivos externos adicionales**.
   - **SafeTensors:**
     - Desarrollado por Hugging Face para almacenar tensores numpy/torch de forma rápida y segura.
     - Solo contiene los tensores de pesos numéricos; **no contiene el tokenizador**.
     - Requiere que el usuario proporcione un archivo `tokenizer.json` o `vocab.json` compatible para convertir texto a tokens.

3. **Ecosistema de Motores Nativos:**
   - **C++ (`llama.cpp` / NDK):**
     - Ideal para procesadores ARM64 usando instrucciones **NEON SIMD**.
     - Aprovecha mapeo de memoria `mmap` para evaluar modelos capa por capa sin desbordar la memoria RAM física del teléfono.
   - **Rust (`Candle` / UniFFI):**
     - Enfoque moderno para manipular tensores SafeTensors con seguridad de memoria estricta y sin overhead de recolector de basura.
   - **Kotlin VM:**
     - Capa de orquestación, gestión de UI reactiva en Jetpack Compose, diálogos y ciclo de vida de actividades.

---

## 📱 Consideraciones de Hardware Móvil

- **Smartphones de entrada (3-4 GB RAM):**
  - Modelos recomendados: **135M a 500M** de parámetros (SmolLM-360M, Qwen2.5-0.5B).
  - Velocidad típica: 35 a 60 tokens por segundo.
- **Smartphones de gama media (6-8 GB RAM):**
  - Modelos recomendados: **1B a 2B** de parámetros (Llama-3.2-1B, Gemma-2-2B, Qwen-2.5-1.5B, DeepSeek-R1-1.5B).
  - Velocidad típica: 18 a 35 tokens por segundo.
- **Smartphones de gama alta (12+ GB RAM):**
  - Modelos recomendados: **3.8B a 7B** de parámetros (Phi-3-Mini, Llama-3.1-8B Q4).
  - Velocidad típica: 10 a 16 tokens por segundo.

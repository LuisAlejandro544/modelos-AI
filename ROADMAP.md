# 🗺️ Roadmap de Desarrollo

Este documento detalla el estado actual del desarrollo y las metas para la aplicación de IA Local en Android.

---

## 🟢 Fase 1: Fundamentos y UI Reactiva (Completada)
- [x] Interfaz de chat moderna y fluida con **Jetpack Compose** y **Material 3**.
- [x] Eliminación de modelos simulados y presets artificiales.
- [x] Arquitectura de importación 100% local y offline para modelos del usuario.
- [x] Diálogo interactivo de parámetros (Temperatura, Top-P, Repeat Penalty, Hilos de CPU).
- [x] Pruebas automatizadas unitarias, de integración y de interfaz con **Robolectric** y **Roborazzi**.

---

## 🟢 Fase 2: Flujo Dual de Carga e Inferencia Móvil Real (Completada)
- [x] **Motor Nativo C++ llama.cpp con Soporte GGUF (v2 y v3):**
  - [x] Parser binario nativo C++ (`gguf_parser.cpp` / `gguf_types.h`) con lectura zero-copy de cabecera `0x46554747`.
  - [x] Extracción en milisegundos de arquitectura (`llama`, `qwen2`, `phi3`, `gemma2`), longitud de contexto y tokens especiales BOS/EOS.
  - [x] Carga nativa mediante Android File Descriptors (`ParcelFileDescriptor`) y memoria virtual `mmap`.
  - [x] Control nativo de cancelación de inferencia (`std::atomic<bool> isCancelled`).
  - [x] Enlace JNI y streaming reactivo hacia la UI de Compose con cálculo de métricas en vivo.
- [x] **Modo SafeTensors Modular (4 Archivos Obligatorios):** Pantalla dedicada con carga obligatoria de tensores (`*.safetensors`), tokenizador (`tokenizer.json`), arquitectura (`config.json`) y plantilla de chat (`tokenizer_config.json`).
- [x] **Motor Nativo Hugging Face Candle en Rust:** Inferencia y forward pass real con multiplicación matricial de embeddings y `lm_head`, decodificación BPE y muestreo `LogitsProcessor`.
- [x] **Extracción Automática de Metadatos:** Auto-detección de capas, parámetros, cuantización y plantillas ChatML/Llama3/Gemma desde JSON y cabeceras binarias.
- [x] **Contador de Tokens y Medidor de Contexto:** Monitoreo en tiempo real del tamaño de la conversación vs. el límite de la ventana de contexto.
- [x] **Medidor de Velocidad de Tokens por Segundo (t/s):** Contador en vivo durante el streaming y estadísticas de rendimiento post-generación.
- [x] **Acelerador de Hardware Seleccionable (GPU / NPU / CPU):** Conmutación / fallback automático a GPU (Vulkan) si el dispositivo no cuenta con NPU física.
- [x] **Mapeo de Memoria Optimizado (`mmap`):** Carga perezosa desde memoria flash para reducir drásticamente el uso de RAM física.
- [x] **Pipeline CI/CD con Caché de Alta Velocidad:** Workflow de GitHub Actions con `rust-cache@v2`, CMake NDK y compilación multi-ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`).

---

## 🟢 Fase 2.5: Tokenización BPE / SentencePiece Nativa en C++ (Completada)
- [x] **Desempaquetado de Vocabulario y Merges GGUF en C++:**
  - [x] Lectura de arreglos `tokenizer.ggml.tokens`, `tokenizer.ggml.merges`, `tokenizer.ggml.scores` y `tokenizer.ggml.token_type` directamente desde metadatos GGUF en memoria mapeada.
  - [x] Extracción de tokens especiales (`<s>`, `</s>`, `<|im_start|>`, `<|im_end|>`, `[INST]`, `<think>`, etc.).
- [x] **Algoritmo de Tokenización BPE y SentencePiece en C++ (`bpe_tokenizer.cpp` / `bpe_tokenizer.h`):**
  - [x] Codificación de texto UTF-8 a secuencia de IDs de tokens mediante fusiones iterativas por rango de prioridad de merges.
  - [x] Soporte para prefijo y sustitución de espacios SentencePiece (` ` / `\u2581`).
  - [x] Mecanismo de byte-fallback bidireccional (`<0xNN>` a bytes crudos y viceversa) para caracteres no contemplados en el vocabulario base.
  - [x] Decodificación de secuencias de tokens e IDs individuales a cadenas de texto UTF-8 coherentes.
- [x] **Integración JNI Completa:**
  - [x] Métodos `tokenizeNative`, `decodeTokensNative` y `decodeTokenNative` en `NativeCppBridge.kt` y `local_ai_engine.cpp`.
  - [x] Conexión directa en el contexto de ejecución nativo (`GgufExecutionContext`).

---

## 🟢 Fase 2.6: Dequantización y Forward Pass del Transformer en C++ (Completada)
- [x] **Multiplicación Matricial y Dequantización en C++ (`dequant_matmul.h`):**
  - [x] Bucle de dequantización y MatMul para bloques `Q4_0`, `Q8_0` y `Q4_K` con conversión flotante FP16->FP32.
  - [x] Multiplicación vector-matriz acelerada por instrucciones vectoriales **ARM NEON** (`arm64-v8a` con `vmlaq_f32`, `vld1q_f32`).
- [x] **Capas del Transformer y Forward Pass en C++ (`transformer_forward.h`):**
  - [x] Capa de Embedding Lookup con dequantización de filas de pesos por ID de token.
  - [x] Normalización **RMSNorm** (`rmsNorm`) para pre/post atención y pre/post FFN.
  - [x] Proyecciones de Atención (Q, K, V) con incrustación posicional rotacional **RoPE** (`applyRope`).
  - [x] Capa Feed-Forward SwiGLU / MLP con función de activación `SiLU` (`silu(x) = x * sigmoid(x)`).
  - [x] Proyección final LM Head (`output.weight`) para generación de Logits por capa sobre el vocabulario.

---

## 🟢 Fase 2.7: Muestreo de Logits y Streaming Token por Token en C++ (Completada)
- [x] **Muestreo de Logits en C++ (`sampler.h`):**
  - [x] Implementación de **Penalización por Repetición** (*Repeat Penalty*) sobre tokens recientes.
  - [x] Normalización de temperatura y cálculo de probabilidades probabilísticas con **Softmax**.
  - [x] Filtros combinados **Top-K** y **Top-P (Nucleus Sampling)** con generación pseudoaleatoria `std::mt19937`.
- [x] **Bucle Autorregresivo de Generación y Streaming JNI Token por Token:**
  - [x] Método nativo `generateStreamingPromptNative` en `local_ai_engine.cpp` y `NativeCppBridge.kt`.
  - [x] Callback JNI reactivo (`NativeTokenCallback.onToken(piece, tokenId)`) para emisión continua de texto sin esperas.
  - [x] Detección de token de parada **EOS** (`</s>`, `<|im_end|>`) y parada interactiva en caliente (`isCancelled`).

---

## 🟡 Fase 3: Persistencia y Gestión Avanzada de Chats (Próxima)
- [ ] Base de datos local **Room** para guardar y reanudar múltiples conversaciones independientes.
- [ ] Exportación de chats a formato Markdown (`.md`) y texto plano.
- [ ] Búsqueda y filtrado de mensajes históricos.
- [ ] Gestión inteligente de KV-Cache para conversaciones largas (truncado o resumen automático).

---

## 🟣 Fase 4: Capacidades Multimodales y RAG Local
- [ ] **RAG Local (Chat con Documentos):** Procesamiento e indexación local de archivos PDF/TXT para responder preguntas sobre documentos sin internet.
- [ ] **Visión Local (VLM):** Soporte para modelos ligeros de visión (ej. Moondream / Llama 3.2 Vision) usando la cámara del teléfono.
- [ ] **Transferencia Wi-Fi Local:** Servidor embebido para transferir modelos GGUF pesados desde una computadora al teléfono sin cables.

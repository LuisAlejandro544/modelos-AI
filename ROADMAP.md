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

## 🟢 Fase 2: Flujo Dual de Carga e Inferencia Móvil Real (Completada / En Refinamiento)
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

## 🟡 Fase 2.5: Motor GGUF Autorregresivo Completo en C++ (Próxima)
- [ ] **Desempaquetado de Vocabulario Nativo GGUF en C++:**
  - [ ] Lectura del arreglo `tokenizer.ggml.tokens` y tipos de token directamente desde los metadatos parseados en C++.
  - [ ] Implementación de decodificador y tokenizador BPE nativo en C++ para convertir IDs en strings UTF-8.
- [ ] **Multiplicación Matricial y Dequantización en C++:**
  - [ ] Bucle de dequantización para bloques `Q4_0`, `Q4_K_M`, `Q5_K_M` y `Q8_0` acelerado por instrucciones vectoriales ARM NEON (`arm64-v8a`).
  - [ ] Forward pass por capas de atención (`blk.N.attn_q.weight`, `blk.N.attn_k.weight`, etc.) y normalización RMSNorm en C++.
- [ ] **Muestreo de Logits y Emisión Token por Token (Sampling Loop C++):**
  - [ ] Implementación de Softmax, muestreo con Temperatura, Top-P y penalización de repetición nativa en C++.
  - [ ] Streaming directo token por token a la JVM mediante callback JNI continuo durante la generación.

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

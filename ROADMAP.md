# Roadmap del Proyecto AI Local (Android)

Este documento describe la visión, fases de desarrollo e hitos técnicos para evolucionar el motor de inferencia local en Android.

---

## 🎯 Fase 1: Arquitectura Base e Interfaz de Usuario (Completada ✅)
- [x] Diseño de interfaz Material Design 3 con paleta cálida y navegación fluida.
- [x] Pantalla de bienvenida con diagnósticos de hardware (núcleos CPU, memoria disponible).
- [x] Selector de modelos locales con filtrado por tamaño de parámetros y cuantización.
- [x] Panel de configuración de parámetros de inferencia (temperatura, top-p, hilos CPU, repeat penalty).
- [x] Generación de respuestas en streaming con métricas en tiempo real (tok/s, latencia, RAM).

## 🎯 Fase 2: Soporte Multi-Formato y Puentes Nativos (Completada ✅)
- [x] Integración de capas nativas C++17 (`CMakeLists.txt` + JNI bridge `NativeCppBridge.kt`).
- [x] Integración de capas nativas Rust 2021 (`Cargo.toml` + `lib.rs` + `RustInferenceBridge.kt`).
- [x] Selector de backend de ejecución (C++ `llama.cpp` vs. Rust `Candle` vs. Kotlin VM).
- [x] Sistema de importación de modelos de usuario en formatos `.gguf` y `.safetensors`.
- [x] Soporte para modelos ultraligeros (135M, 360M, 500M, 0.6B) y sub-4B.
- [x] Guía integrada en la app sobre el uso de `tokenizer.json` / `vocab.json` vs. GGUF.

## 🎯 Fase 3: Aceleración por Hardware y Memoria (Próximos Pasos 🚧)
- [ ] **Mapeo de Memoria (`mmap`) Avanzado:** Apertura directa de archivos GGUF mediante `FileDescriptor` para reducir el consumo de RAM en dispositivos con 3-4 GB.
- [ ] **Aceleración GPU por Vulkan / OpenCL:** Enlazar shaders de cómputo en C++ para derivar el cálculo de capas densas a la GPU Adreno / Mali.
- [ ] **Soporte NNAPI / MediaPipe LLM:** Ejecución opcional sobre NPUs móviles (Qualcomm Hexagon y MediaTek APU).
- [ ] **Gestor de Descarga Integrado (Hugging Face Hub):** Descargar modelos `.gguf` directamente desde la app con barra de progreso, pausa y reanudación.

## 🎯 Fase 4: Persistencia y Herramientas Avanzadas (Futuro 🔮)
- [ ] **Persistencia de Sesiones en Room Database:** Guardar conversaciones múltiples, historial de chats y biblioteca de modelos importados.
- [ ] **RAG Local (Retrieval-Augmented Generation):** Ingesta de archivos PDF/TXT locales mediante embeddings vectoriales offline.
- [ ] **Servidor HTTP Local (Wi-Fi Transfer):** Subir archivos `.gguf` desde el navegador del PC a la memoria del móvil sin cables.
- [ ] **Soporte para Modelos de Visión (VLM):** Inferencia multimodal local (ej: Moondream 2 / Llama-3.2-Vision 3B).

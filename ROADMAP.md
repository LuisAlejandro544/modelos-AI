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
- [x] **Modo SafeTensors Modular (4 Archivos Obligatorios):** Pantalla dedicada con carga de tensores (`*.safetensors`), tokenizador (`tokenizer.json`), arquitectura (`config.json`) y plantilla de chat (`tokenizer_config.json`).
- [x] **Motor Nativo Hugging Face Candle en Rust:** Inferencia y forward pass real con multiplicación matricial de embeddings y `lm_head`, decodificación BPE y muestreo `LogitsProcessor`.
- [x] **Extracción Automática de Metadatos:** Auto-detección de capas, parámetros, cuantización y plantillas ChatML/Llama3/Gemma desde JSON y cabeceras binarias.
- [x] **Contador de Tokens y Medidor de Contexto:** Monitoreo en tiempo real del tamaño de la conversación vs. el límite de la ventana de contexto.
- [x] **Medidor de Velocidad de Tokens por Segundo (t/s):** Contador en vivo durante el streaming y estadísticas de rendimiento post-generación.
- [x] **Acelerador de Hardware Seleccionable (GPU / NPU / CPU):** Conmutación / fallback automático a GPU (Vulkan) si el dispositivo no cuenta con NPU física.
- [x] **Mapeo de Memoria Optimizado (`mmap`):** Carga perezosa desde memoria flash para reducir drásticamente el uso de RAM física.
- [x] **Pipeline CI/CD con Caché de Alta Velocidad y Empaquetado 7z Ultra:** Workflow de GitHub Actions con `rust-cache@v2`, CMake NDK, compilación multi-ABI y compresión con **7-Zip Ultra (LZMA2)** para descargas reducidas en conexiones móviles.

---

## 🟢 Fase 2.5 a 2.8: Estabilización, Tokenización C++ y Control de Parámetros (Completada)
- [x] **Tokenización BPE / SentencePiece Nativa en C++ (`bpe_tokenizer.cpp` / `bpe_tokenizer.h`):** Desempaquetado de vocabulario y merges GGUF en memoria mapeada con byte-fallback.
- [x] **Dequantización y Forward Pass en C++ (`dequant_matmul.h` / `transformer_forward.h`):** Cuantizaciones Q4/Q8 aceleradas con ARM NEON SIMD, RMSNorm, RoPE y SwiGLU.
- [x] **Muestreo Seguro y UTF-8 Streaming (`sampler.h`, `streaming_engine.cpp`, `utf8_util.h`):** Acumulador Modified-UTF-8 para evitar errores JNI en tokens multibyte y límites numéricos en logits.
- [x] **Sanitización de Parámetros:** Clamping automático de contexto y parámetros numéricos (`InferenceParameters.sanitize()`).

---

## 🟢 Fase 2.9: Rediseño M3, Telemetría Real, Persistencia de Modelos y Edición SafeTensors (Completada)
- [x] **Rediseño Completo de la Pantalla Principal:** Eliminación del hero banner y diseño de cabecera limpia, espaciosa y accesible con tipografía Material Design 3.
- [x] **Telemetría en Vivo de Hardware Real:**
  - [x] Detección de modelo, fabricante (`Build.MANUFACTURER`, `Build.MODEL`) y SoC (`Build.SOC_MODEL` / Chipset).
  - [x] Lectura de RAM real libre y total en GB (`ActivityManager.MemoryInfo`).
  - [x] Lectura de almacenamiento libre disponible en almacenamiento interno (`StatFs`).
  - [x] Detección de núcleos de CPU, ABI nativa y estado de aceleración GPU/NPU/NEON.
- [x] **Biblioteca de Modelos Guardados y Persistencia:**
  - [x] Persistencia automática en base de datos local SQLite/Room (`LocalAiDatabase`, `ModelEntity`).
  - [x] Lista de modelos guardados directamente en la pantalla de bienvenida y en el selector modal.
  - [x] Acción de inicio de conversación rápida en 1 toque.
- [x] **Edición y Complementación Modular de SafeTensors:**
  - [x] Interfaz de edición para modelos SafeTensors guardados que permite añadir o sustituir archivos faltantes (`tokenizer.json`, `config.json`, `tokenizer_config.json`, `generation_config.json`, prompt).
  - [x] Sincronización inmediata con Room.

---

## 🟢 Fase 3: Administrador e Historial Completo de Chats con Room (Completada)
- [x] **Inicio de Chat Limpio al Importar Modelo:** Creación automática de una sesión limpia desde 0 (`messages = emptyList()`) al importar un archivo GGUF o configurar un paquete SafeTensors.
- [x] **Base de Datos Local Room para Sesiones y Mensajes:**
  - [x] Entidad `ChatSessionEntity` y `ChatMessageEntity` con relaciones y DAO (`ChatDao`).
  - [x] Guardado reactivo de prompts de usuario y respuestas del asistente con sus métricas (tok/s, latencia, hardware).
- [x] **Diálogo de Historial de Chats:**
  - [x] Explorador de conversaciones guardadas con timestamp, modelo utilizado, número de mensajes y previsualización.
  - [x] Acción "+ Iniciar Nueva Conversación".
  - [x] Reanudar conversaciones pasadas al instante.
  - [x] Renombrar y eliminar sesiones de chat con confirmación.
- [x] **Sección de Conversaciones Recientes:** Visualización directa en la pantalla principal para reanudación rápida.

---

## 🟡 Fase 4: Exportación y Gestión de KV-Cache (Próxima)
- [ ] Exportación de chats a formato Markdown (`.md`) y texto plano para compartir.
- [ ] Búsqueda y filtrado de mensajes históricos por palabra clave.
- [ ] Gestión inteligente de KV-Cache para conversaciones largas (truncado deslizante y compresión).

---

## 🟣 Fase 5: Capacidades Multimodales y RAG Local
- [ ] **RAG Local (Chat con Documentos):** Procesamiento e indexación local de archivos PDF/TXT para responder preguntas sobre documentos sin internet.
- [ ] **Visión Local (VLM):** Soporte para modelos ligeros de visión (ej. Moondream / Llama 3.2 Vision) usando la cámara del teléfono.
- [ ] **Transferencia Wi-Fi Local:** Servidor embebido para transferir modelos GGUF pesados desde una computadora al teléfono sin cables.

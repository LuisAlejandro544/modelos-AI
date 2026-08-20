# 📂 Estructura del Proyecto

Organización modular del código fuente de la aplicación Android.

```
/
├── .github/workflows/
│   ├── build-apk.yml               # Workflow de GitHub Actions para compilar APK
│   └── override-commit.yml         # Workflow de GitHub Actions para sincronizar mensaje de commit
├── app/
│   ├── build.gradle.kts            # Configuración de dependencias, Room, KSP, NDK y CMakeLists
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── cpp/                # Motor nativo C++ modular (llama.cpp NDK / GGUF v2/v3 / Tokenizador BPE/SPM / MatMul NEON / Forward Pass / Sampler)
│   │   │   │   ├── CMakeLists.txt          # Script de construcción CMake con todos los módulos nativos C++
│   │   │   │   ├── gguf_types.h            # Definición de especificación GGUF, magic 0x46554747, tipos GGML y metadata
│   │   │   │   ├── gguf_parser.h           # Cabecera del parser binario de metadatos GGUF
│   │   │   │   ├── gguf_parser.cpp         # Lector zero-copy mmap de arquitectura, tensores, vocabulario y merges
│   │   │   │   ├── bpe_tokenizer.h         # Cabecera del tokenizador nativo C++ (BPE, SentencePiece, byte fallback y tokens especiales)
│   │   │   │   ├── bpe_tokenizer.cpp       # Algoritmo de codificación/decodificación BPE/SentencePiece y merges
│   │   │   │   ├── dequant_matmul.h        # Rutinas de decuantización Q4_0, Q8_0, Q4_K y MatMul vectorizado ARM NEON
│   │   │   │   ├── transformer_forward.h   # Capas del Transformer (RMSNorm, RoPE, Attention, SwiGLU/FFN y LM Head)
│   │   │   │   ├── sampler.h               # Muestreador de logits (Temperatura, Top-P, Top-K, Repeat Penalty y Softmax)
│   │   │   │   ├── utf8_util.h             # Sanitizador Modified-UTF-8 para evitar excepciones JNI
│   │   │   │   ├── context_manager.h       # Gestor modular de contextos de ejecución GGUF (thread-safe handles)
│   │   │   │   ├── context_manager.cpp     # Asignación mmap, ciclo de vida de handles y liberación de memoria C++
│   │   │   │   ├── streaming_engine.h      # Motor de evaluación y streaming autorregresivo desacoplado
│   │   │   │   ├── streaming_engine.cpp    # Bucle autorregresivo con acumulador UTF-8 y dispatch de callbacks JNI
│   │   │   │   └── local_ai_engine.cpp     # Puntos de entrada JNI limpios que delegan a ContextManager y StreamingEngine
│   │   │   ├── rust/               # Motor nativo Rust (Candle / UniFFI)
│   │   │   │   ├── Cargo.toml
│   │   │   │   └── src/
│   │   │   │       ├── lib.rs              # Punto de entrada y exportaciones de módulos en Rust
│   │   │   │       ├── engine.rs           # Bucle autoregresivo Candle, RMSNorm y forward pass
│   │   │   │       ├── model_loader.rs     # Deserialización SafeTensors (mmap con ParcelFileDescriptor)
│   │   │   │       ├── sampler.rs          # Control de interrupción y sampling de logits
│   │   │   │       └── jni_bridge.rs       # Métodos nativos JNI con Android JVM
│   │   │   ├── java/com/example/
│   │   │   │   ├── App.kt          # Aplicación Android con singleton de contexto
│   │   │   │   ├── MainActivity.kt # Actividad principal y navegación Compose
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/              # Capa de persistencia local offline con Room
│   │   │   │   │   │   ├── LocalAiDatabase.kt      # Base de datos Room (RoomDatabase singleton)
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── ModelDao.kt         # DAO para modelos importados GGUF/SafeTensors
│   │   │   │   │   │   │   └── ChatDao.kt          # DAO para sesiones y mensajes de chat
│   │   │   │   │   │   └── entities/
│   │   │   │   │   │       ├── ModelEntity.kt      # Entidad Room para catálogo persistente de modelos
│   │   │   │   │   │       ├── ChatSessionEntity.kt# Entidad Room para sesiones de conversación
│   │   │   │   │   │       └── ChatMessageEntity.kt# Entidad Room para mensajes con métricas
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── ModelRepository.kt  # Repositorio Room para alta, edición y baja de modelos
│   │   │   │   │       └── ChatRepository.kt   # Repositorio Room para historial, creación y borrado de chats
│   │   │   │   ├── engine/
│   │   │   │   │   ├── LocalInferenceEngine.kt       # Coordinador central de inferencia local y streaming
│   │   │   │   │   ├── NativeCppBridge.kt            # JNI C++ (llama.cpp, NEON, Vulkan, tokenizador BPE/SPM nativo)
│   │   │   │   │   ├── RustInferenceBridge.kt        # JNI Rust (Candle, memoria segura y ParcelFileDescriptor)
│   │   │   │   │   ├── tokenizer/
│   │   │   │   │   │   └── TextDetokenizer.kt        # Sanitización y desmapeo universal de bytes UTF-8 (GPT-2, SPM, hex)
│   │   │   │   │   ├── utils/
│   │   │   │   │   │   └── FileDescriptorResolver.kt # Apertura segura de ParcelFileDescriptor (content:// y paths)
│   │   │   │   │   ├── formatter/
│   │   │   │   │   │   └── ChatTemplateFormatter.kt  # Formateo de plantillas de chat (ChatML, Llama-3, Gemma, Mistral)
│   │   │   │   │   ├── hardware/
│   │   │   │   │   │   └── HardwareCapabilityDetector.kt # Telemetría real de SoC, RAM libre/total, almacenamiento, ABI y NPU
│   │   │   │   │   └── metrics/
│   │   │   │   │       └── InferenceMetricsTracker.kt    # Cálculo en tiempo real de tokens/segundo, latencia y contexto
│   │   │   │   ├── model/
│   │   │   │   │   ├── ChatMessage.kt                # Mensajes con métricas y estado en vivo
│   │   │   │   │   ├── InferenceParameters.kt        # HardwareAccelerator, InferenceBackend (C++, Rust), mmap y sanitize()
│   │   │   │   │   └── LocalModel.kt                 # Definición de modelos GGUF/SafeTensors y formatos
│   │   │   │   ├── ui/
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatScreen.kt             # Pantalla orquestadora de chat y diálogos
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── ChatTopBar.kt             # Barra superior con historial, chip de modelo, parámetros y limpiar
│   │   │   │   │   │       ├── ContextMeterBar.kt        # Barra de progreso de tokens, contexto y t/s en vivo
│   │   │   │   │   │       ├── ChatInputBar.kt           # Barra de entrada de texto, botón Stop/Send e indicador Offline
│   │   │   │   │   │       ├── ChatMessageBubble.kt      # Burbujas de mensajes con métricas y botón de copiado
│   │   │   │   │   │       └── ChatWelcomeSuggestions.kt # Estado inicial con sugerencias de prompts
│   │   │   │   │   ├── dialogs/
│   │   │   │   │   │   ├── ChatHistoryDialog.kt      # Diálogo modal de gestión e historial de chats (cambiar, renombrar, borrar)
│   │   │   │   │   │   ├── ImportModelDialog.kt      # Diálogo de importación multiformato (GGUF / SafeTensors)
│   │   │   │   │   │   ├── ModelSelectorDialog.kt    # Selector de modelos guardados con soporte de edición y borrado
│   │   │   │   │   │   ├── ParametersDialog.kt       # Selector GPU/NPU/CPU, toggle mmap, contexto dinámico y backends (C++/Rust)
│   │   │   │   │   │   └── TokenizerGuideDialog.kt   # Guía de compatibilidad de archivos (GGUF vs SafeTensors)
│   │   │   │   │   ├── safetensors/
│   │   │   │   │   │   ├── SafeTensorsImportScreen.kt# Pantalla de importación y edición modular de SafeTensors
│   │   │   │   │   │   ├── components/
│   │   │   │   │   │   │   ├── FilePickerCard.kt         # Selector de archivo individual con estado y acciones
│   │   │   │   │   │   │   ├── SafeTensorsHeaderCard.kt  # Banner informativo de archivos requeridos y metadatos
│   │   │   │   │   │   │   └── SafeTensorsMetadataForm.kt# Formulario de parámetros, cuantización y botón de inicio
│   │   │   │   │   │   └── parser/
│   │   │   │   │   │       └── ModelConfigParser.kt      # Parser JSON de config.json y tokenizer_config.json
│   │   │   │   │   ├── theme/                        # Colores, tipografía y tema Material 3
│   │   │   │   │   └── welcome/
│   │   │   │   │       └── WelcomeScreen.kt          # Pantalla principal con telemetría, modelos guardados y chats recientes
│   │   │   │   └── viewmodel/
│   │   │   │       ├── ChatUiState.kt                # Estado inmutable de la UI, sesiones de chat y cálculo de contexto
│   │   │   │       └── ChatViewModel.kt              # Orquestador ligero de UI, corrutinas, repositorio Room y eventos
│   │   │   └── res/                                  # Drawables, mipmaps, strings y temas XML
│   │   └── test/                                     # Tests unitarios y Robolectric
│   │       ├── java/com/example/
│   │       │   ├── ExampleUnitTest.kt                # Pruebas unitarias de Formatter, Metrics, Tokenizer, Entities y Repository
│   │       │   ├── ExampleRobolectricTest.kt         # Pruebas de integración Robolectric y Room
│   │       │   └── GreetingScreenshotTest.kt         # Pruebas de captura Roborazzi
│   ├── AGENTS.md                       # Directivas obligatorias para agentes de IA
│   ├── AI_CONTEXT.md                   # Resumen técnico y arquitectura para agentes
│   ├── commit_message.txt              # Mensaje descriptivo para el commit automático
│   ├── README.md                       # Documentación principal del proyecto
│   ├── ROADMAP.md                      # Plan de ruta y fases del proyecto
│   └── STRUCTURE.md                    # Este archivo
```

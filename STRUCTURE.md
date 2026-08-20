# 📂 Estructura del Proyecto

Organización modular del código fuente de la aplicación Android.

```
/
├── .github/workflows/
│   ├── build-apk.yml               # Workflow de GitHub Actions para compilar APK
│   └── override-commit.yml         # Workflow de GitHub Actions para sincronizar mensaje de commit
├── app/
│   ├── build.gradle.kts            # Configuración de dependencias, NDK (ABIs arm64-v8a/armeabi-v7a/x86_64) y CMakeLists
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── cpp/                # Motor nativo C++ (llama.cpp NDK / CMake / GGUF v2/v3 / Tokenizador BPE/SPM / MatMul NEON / Forward Pass / Sampler)
│   │   │   │   ├── CMakeLists.txt          # Script de construcción CMake para C++
│   │   │   │   ├── gguf_types.h            # Definición de especificación GGUF, magic 0x46554747, tipos GGML y metadata
│   │   │   │   ├── gguf_parser.h           # Cabecera del parser binario de metadatos GGUF
│   │   │   │   ├── gguf_parser.cpp         # Lector zero-copy mmap de arquitectura, tensores, vocabulario y merges
│   │   │   │   ├── bpe_tokenizer.h         # Cabecera del tokenizador nativo C++ (BPE, SentencePiece, byte fallback y tokens especiales)
│   │   │   │   ├── bpe_tokenizer.cpp       # Algoritmo de codificación/decodificación BPE/SentencePiece y merges
│   │   │   │   ├── dequant_matmul.h        # Rutinas de decuantización Q4_0, Q8_0, Q4_K y MatMul vectorizado ARM NEON
│   │   │   │   ├── transformer_forward.h   # Capas del Transformer (RMSNorm, RoPE, Attention, SwiGLU/FFN y LM Head)
│   │   │   │   ├── sampler.h               # Muestreador de logits (Temperatura, Top-P, Top-K, Repeat Penalty y Softmax)
│   │   │   │   └── local_ai_engine.cpp     # Enlaces JNI C++, bucle autorregresivo, streaming token por token y cancelación
│   │   │   ├── rust/               # Motor nativo Rust (Candle / UniFFI)
│   │   │   │   ├── Cargo.toml
│   │   │   │   └── src/
│   │   │   │       ├── lib.rs              # Punto de entrada y exportaciones de módulos en Rust
│   │   │   │       ├── engine.rs           # Bucle autoregresivo Candle, RMSNorm y forward pass
│   │   │   │       ├── model_loader.rs     # Deserialización SafeTensors (mmap con ParcelFileDescriptor)
│   │   │   │       ├── sampler.rs          # Control de interrupción y sampling de logits
│   │   │   │       └── jni_bridge.rs       # Métodos nativos JNI con Android JVM
│   │   │   ├── java/com/example/
│   │   │   │   ├── App.kt          # Aplicación Android con proveedor global de ContentResolver
│   │   │   │   ├── MainActivity.kt # Actividad principal y navegación Compose
│   │   │   │   ├── data/
│   │   │   │   │   └── repository/
│   │   │   │   │       └── ModelRepository.kt        # Gestión reactiva y ciclo de vida de modelos locales (GGUF / SafeTensors)
│   │   │   │   ├── engine/
│   │   │   │   │   ├── LocalInferenceEngine.kt       # Coordinador central de inferencia local y streaming
│   │   │   │   │   ├── NativeCppBridge.kt            # JNI C++ (llama.cpp, NEON, Vulkan, tokenizador BPE/SPM nativo)
│   │   │   │   │   ├── RustInferenceBridge.kt        # JNI Rust (Candle, memoria segura y ParcelFileDescriptor)
│   │   │   │   │   ├── formatter/
│   │   │   │   │   │   └── ChatTemplateFormatter.kt  # Formateo de plantillas de chat (ChatML, Llama-3, Gemma, Mistral)
│   │   │   │   │   ├── hardware/
│   │   │   │   │   │   └── HardwareCapabilityDetector.kt # Detección de specs, aceleradores (GPU/NPU/CPU) y memoria
│   │   │   │   │   └── metrics/
│   │   │   │   │       └── InferenceMetricsTracker.kt    # Cálculo en tiempo real de tokens/segundo, latencia y contexto
│   │   │   │   ├── model/
│   │   │   │   │   ├── ChatMessage.kt                # Mensajes con métricas y estado en vivo
│   │   │   │   │   ├── InferenceParameters.kt        # HardwareAccelerator, mmap, contexto y parámetros
│   │   │   │   │   └── LocalModel.kt                 # Definición de modelos GGUF/SafeTensors y formatos
│   │   │   │   ├── ui/
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatScreen.kt             # Pantalla orquestadora de chat y diálogos
│   │   │   │   │   │   └── components/
│   │   │   │   │   │       ├── ChatTopBar.kt             # Barra superior con chip de modelo, parámetros y limpiar
│   │   │   │   │   │       ├── ContextMeterBar.kt        # Barra de progreso de tokens, contexto y t/s en vivo
│   │   │   │   │   │       ├── ChatInputBar.kt           # Barra de entrada de texto, botón Stop/Send e indicador Offline
│   │   │   │   │   │       ├── ChatMessageBubble.kt      # Burbujas de mensajes con métricas y botón de copiado
│   │   │   │   │   │       └── ChatWelcomeSuggestions.kt # Estado inicial con sugerencias de prompts
│   │   │   │   │   ├── dialogs/
│   │   │   │   │   │   ├── ImportModelDialog.kt      # Diálogo de importación de modelos de usuario
│   │   │   │   │   │   ├── ModelSelectorDialog.kt    # Selector de modelos
│   │   │   │   │   │   ├── ParametersDialog.kt       # Selector GPU/NPU/CPU, toggle mmap, contexto y sliders
│   │   │   │   │   │   └── TokenizerGuideDialog.kt   # Guía de compatibilidad de tokenizadores
│   │   │   │   │   ├── safetensors/
│   │   │   │   │   │   ├── SafeTensorsImportScreen.kt# Pantalla orquestadora de importación SafeTensors
│   │   │   │   │   │   ├── components/
│   │   │   │   │   │   │   ├── FilePickerCard.kt         # Selector de archivo individual con estado y acciones
│   │   │   │   │   │   │   ├── SafeTensorsHeaderCard.kt  # Banner informativo de archivos requeridos y metadatos
│   │   │   │   │   │   │   └── SafeTensorsMetadataForm.kt# Formulario de parámetros, cuantización y botón de inicio
│   │   │   │   │   │   └── parser/
│   │   │   │   │   │       └── ModelConfigParser.kt      # Parser JSON de config.json y tokenizer_config.json
│   │   │   │   │   ├── theme/                        # Colores, tipografía y tema Material 3
│   │   │   │   │   └── welcome/
│   │   │   │   │       └── WelcomeScreen.kt          # Pantalla inicial con specs de hardware y aceleración
│   │   │   │   └── viewmodel/
│   │   │   │       ├── ChatUiState.kt                # Estado inmutable de la UI, pantallas y cálculo de contexto
│   │   │   │       └── ChatViewModel.kt              # Orquestador ligero de UI, corrutinas y eventos
│   │   │   └── res/                                  # Drawables, mipmaps, strings y temas XML
│   │   └── test/                                     # Tests unitarios y Robolectric
│   │       ├── java/com/example/
│   │       │   ├── ExampleUnitTest.kt                # Pruebas unitarias de Formatter, Metrics y Repository
│   │       │   └── ExampleRobolectricTest.kt         # Pruebas de integración Robolectric
│   ├── AGENTS.md                       # Directivas obligatorias para agentes de IA
│   ├── AI_CONTEXT.md                   # Resumen técnico y arquitectura para agentes
│   ├── commit_message.txt              # Mensaje descriptivo para el commit automático
│   ├── README.md                       # Documentación principal del proyecto
│   ├── ROADMAP.md                      # Plan de ruta y fases del proyecto
│   └── STRUCTURE.md                    # Este archivo
```

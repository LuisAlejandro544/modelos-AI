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
│   │   │   ├── cpp/                # Motor nativo C++ (llama.cpp NDK / CMake)
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   └── local_ai_engine.cpp
│   │   │   ├── rust/               # Motor nativo Rust (Candle / UniFFI)
│   │   │   │   ├── Cargo.toml
│   │   │   │   └── src/lib.rs
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt # Actividad principal y navegación Compose
│   │   │   │   ├── data/
│   │   │   │   │   └── repository/
│   │   │   │   │       └── ModelRepository.kt        # Gestión reactiva y ciclo de vida de modelos locales (GGUF / SafeTensors)
│   │   │   │   ├── engine/
│   │   │   │   │   ├── LocalInferenceEngine.kt       # Coordinador central de inferencia local y streaming
│   │   │   │   │   ├── NativeCppBridge.kt            # JNI C++ (llama.cpp, NEON, Vulkan, mmap)
│   │   │   │   │   ├── RustInferenceBridge.kt        # JNI Rust (Candle, memoria segura)
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
│   │   │   │   │   │   └── ChatScreen.kt             # Pantalla de chat, medidor de tokens, barra de contexto y t/s
│   │   │   │   │   ├── dialogs/
│   │   │   │   │   │   ├── ImportModelDialog.kt      # Diálogo de importación de modelos de usuario
│   │   │   │   │   │   ├── ModelSelectorDialog.kt    # Selector de modelos
│   │   │   │   │   │   ├── ParametersDialog.kt       # Selector GPU/NPU/CPU, toggle mmap, contexto y sliders
│   │   │   │   │   │   └── TokenizerGuideDialog.kt   # Guía de compatibilidad de tokenizadores
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
├── AGENTS.md                       # Directivas obligatorias para agentes de IA
├── AI_CONTEXT.md                   # Resumen técnico y arquitectura para agentes
├── commit_message.txt              # Mensaje descriptivo para el commit automático
├── README.md                       # Documentación principal del proyecto
├── ROADMAP.md                      # Plan de ruta y fases del proyecto
└── STRUCTURE.md                    # Este archivo
```

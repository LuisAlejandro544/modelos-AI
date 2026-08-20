# 📂 Estructura del Proyecto

Organización modular del código fuente de la aplicación Android.

```
/
├── .github/workflows/
│   ├── build-apk.yml               # Workflow de GitHub Actions para compilar APK
│   └── override-commit.yml         # Workflow de GitHub Actions para sincronizar mensaje de commit
├── app/
│   ├── build.gradle.kts            # Configuración de dependencias y NDK/CMake
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── cpp/                # Motor nativo C++ (llama.cpp NDK)
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   └── local_ai_engine.cpp
│   │   │   ├── rust/               # Motor nativo Rust (Candle)
│   │   │   │   ├── Cargo.toml
│   │   │   │   └── src/lib.rs
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt # Actividad principal y navegación Compose
│   │   │   │   ├── engine/
│   │   │   │   │   ├── LocalInferenceEngine.kt   # Gestor de inferencia, fallback GPU/NPU y streaming t/s
│   │   │   │   │   ├── NativeCppBridge.kt        # JNI C++ (llama.cpp, NEON, Vulkan, mmap)
│   │   │   │   │   └── RustInferenceBridge.kt    # JNI Rust (Candle, memoria segura)
│   │   │   │   ├── model/
│   │   │   │   │   ├── ChatMessage.kt            # Mensajes con métricas y estado en vivo
│   │   │   │   │   ├── InferenceParameters.kt    # HardwareAccelerator, mmap, contexto y parámetros
│   │   │   │   │   └── LocalModel.kt             # Definición de modelos GGUF/SafeTensors y presets
│   │   │   │   ├── ui/
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   └── ChatScreen.kt         # Pantalla de chat, medidor de tokens, barra de contexto y t/s
│   │   │   │   │   ├── dialogs/
│   │   │   │   │   │   ├── ImportModelDialog.kt  # Diálogo de importación de modelos de usuario
│   │   │   │   │   │   ├── ModelSelectorDialog.kt# Selector de modelos
│   │   │   │   │   │   ├── ParametersDialog.kt   # Selector GPU/NPU/CPU, toggle mmap, contexto y sliders
│   │   │   │   │   │   └── TokenizerGuideDialog.kt# Guía de compatibilidad de tokenizadores
│   │   │   │   │   ├── theme/                    # Colores, tipografía y tema Material 3
│   │   │   │   │   └── welcome/
│   │   │   │   │       └── WelcomeScreen.kt      # Pantalla inicial con specs de hardware y aceleración
│   │   │   │   └── viewmodel/
│   │   │   │       └── ChatViewModel.kt          # Gestión de estado, cálculo de tokens y corrutinas
│   │   │   └── res/                              # Drawables, mipmaps, strings y temas XML
│   │   └── test/                                 # Tests Robolectric y Roborazzi
├── AGENTS.md                       # Directivas obligatorias para agentes de IA
├── AI_CONTEXT.md                   # Resumen técnico y arquitectura para agentes
├── commit_message.txt              # Mensaje descriptivo para el commit automático
├── README.md                       # Documentación principal del proyecto
├── ROADMAP.md                      # Plan de ruta y fases del proyecto
└── STRUCTURE.md                    # Este archivo
```

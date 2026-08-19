# Estructura del Proyecto AI Local

Este documento detalla la organización de carpetas, módulos de código, capas nativas y arquitectura del proyecto.

---

## 📂 Árbol de Archivos Principal

```
AI_Local/
├── app/
│   ├── build.gradle.kts                   # Configuración del módulo Android y dependencias
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # Manifiesto de Android (Permisos, Application, Activity)
│   │   │   ├── cpp/                       # ─── Capa Nativa C++ (llama.cpp / NDK) ───
│   │   │   │   ├── CMakeLists.txt         # Configuración CMake para compilar la librería compartida
│   │   │   │   └── local_ai_engine.cpp    # Métodos JNI (ARM NEON, contexto, tensores, evaluación)
│   │   │   ├── rust/                      # ─── Capa Nativa Rust (Candle / UniFFI) ───
│   │   │   │   ├── Cargo.toml             # Manifiesto Cargo con optimizaciones cdylib y LTO
│   │   │   │   └── src/
│   │   │   │       └── lib.rs             # Implementación Rust con seguridad de memoria y JNI
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt        # Entrada principal, Scaffold, transición de pantallas y diálogos
│   │   │   │   ├── engine/                # Motores de inferencia y puentes JNI
│   │   │   │   │   ├── LocalInferenceEngine.kt   # Orquestador reactivo de generación y streaming
│   │   │   │   │   ├── NativeCppBridge.kt         # Puente JNI a liblocal_ai_cpp.so
│   │   │   │   │   └── RustInferenceBridge.kt     # Puente JNI a liblocal_ai_rust.so
│   │   │   │   ├── model/                 # Modelos de dominio y datos
│   │   │   │   │   ├── ChatMessage.kt            # Mensajes, roles (User/Assistant) y métricas
│   │   │   │   │   ├── InferenceParameters.kt    # Parámetros (backend, temperatura, top-p, hilos)
│   │   │   │   │   └── LocalModel.kt             # Definición de modelos, formatos (GGUF/SafeTensors) y presets
│   │   │   │   ├── ui/                    # Capa de presentación (Jetpack Compose M3)
│   │   │   │   │   ├── chat/                     # Pantalla de chat y burbujas de mensajes
│   │   │   │   │   │   └── ChatScreen.kt
│   │   │   │   │   ├── dialogs/                  # Diálogos modales
│   │   │   │   │   │   ├── ImportModelDialog.kt  # Selector e importador de archivos .gguf y .safetensors
│   │   │   │   │   │   ├── ModelSelectorDialog.kt# Lista de modelos propios y preconfigurados
│   │   │   │   │   │   ├── ParametersDialog.kt   # Ajuste de temperatura, CPU threads, backend
│   │   │   │   │   │   └── TokenizerGuideDialog.kt# Guía explicativa sobre tokenizers y Hugging Face
│   │   │   │   │   ├── theme/                    # Sistema de diseño y temas Material 3
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   └── welcome/                  # Pantalla inicial de bienvenida y diagnósticos
│   │   │   │   │       └── WelcomeScreen.kt
│   │   │   │   └── viewmodel/             # Gestión de estado (MVVM)
│   │   │   │       └── ChatViewModel.kt          # StateFlow reactivo, importación y streaming
│   │   │   └── res/                       # Recursos XML, drawables, cadenas e iconos
│   │   └── test/                          # Tests unitarios locales y pruebas Robolectric
│   │       └── java/com/example/
│   │           ├── ExampleRobolectricTest.kt
│   │           └── GreetingScreenshotTest.kt
├── README.md                              # Documentación general del proyecto
├── ROADMAP.md                             # Hitos y visión de desarrollo
├── STRUCTURE.md                           # Estructura del código y archivos
├── AI_CONTEXT.md                          # Contexto técnico para modelos y asistentes AI
└── AGENTS.md                              # Reglas y directivas de desarrollo para agentes
```

---

## 🏛️ Patrón Arquitectónico

- **Patrón Principal:** **MVVM (Model-View-ViewModel)** con flujo unidireccional de datos (**UDF**).
- **Flujo de Estado:** `StateFlow<ChatUiState>` en `ChatViewModel` consumido mediante `collectAsStateWithLifecycle()` en Compose.
- **Flujo de Inferencia:** `Flow<StreamChunk>` emitido por `LocalInferenceEngine` hacia la UI para renderizado token a token en tiempo real.
- **Capa Nativa:** Enlace dinámico mediante `System.loadLibrary()` con envoltorios seguros en Kotlin que gestionan excepciones `UnsatisfiedLinkError` para evitar cierres inesperados.

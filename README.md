# AI Local - Motor de Inferencia On-Device para Android

Aplicación Android nativa en **Kotlin** y **Jetpack Compose** para la ejecución de modelos de lenguaje grande (LLMs) **100% offline y privados** directamente en el hardware de tu teléfono móvil.

---

## ✨ Características Principales

### 🧠 Inferencia Local Multi-Motor
- **Modo GGUF (C++ / Llama.cpp):** Carga directa de modelos todo en uno (`.gguf`) con decuantización rápida en memoria (Q4_K, Q8_0, Q2_K) y mapeo flash `mmap`.
- **Modo SafeTensors (Rust / Candle):** Carga modular de pesos neuronales (`.safetensors`) junto a `tokenizer.json`, `config.json` y plantillas de chat.
- **Edición y Complementación de SafeTensors:** Permite editar configuraciones existentes para añadir o reemplazar archivos faltantes (tokenizador, configs de arquitectura o prompt de sistema).

### 💬 Administrador e Historial de Chats (Room Persistence)
- **Inicio de Chat Limpio al Importar:** Cada vez que se importa o carga un modelo nuevo, se inicia una nueva conversación desde 0 vinculada a dicho modelo.
- **Historial Completo de Conversaciones:** Panel modal para explorar chats pasados, reanudarlos al instante con todo su contexto, renombrarlos o eliminarlos.
- **Persistencia Reactiva en Base de Datos Local:** Cada mensaje enviado y respuesta generada (con sus métricas de velocidad y hardware) se guardan automáticamente en Room SQLite sin depender de servicios en la nube.
- **Previsualización Rápida:** Acceso a conversaciones recientes directamente desde la pantalla de bienvenida.

### 📱 Telemetría Real de Hardware Móvil
- Identificación precisa del dispositivo y fabricante (`Build.MANUFACTURER`, `Build.MODEL`).
- Detección de SoC / Procesador móvil (Snapdragon, Dimensity, Tensor, Exynos) y núcleos de CPU.
- Monitoreo en tiempo real de **RAM disponible / RAM total** (`ActivityManager.MemoryInfo`).
- Medición de almacenamiento libre para modelos (`StatFs`).
- Detección de aceleradores activos: GPU Vulkan, ARM NEON SIMD y NPU (NNAPI).

### 💾 Persistencia y Biblioteca de Modelos (Room Database)
- Todos los modelos importados por el usuario quedan guardados en la base de datos local SQLite/Room.
- Acceso inmediato a tus modelos guardados desde la pantalla principal, con especificaciones, chip de formato y botón de inicio rápido.

### 🎨 Interfaz Moderna Material Design 3 (M3)
- Diseño limpio y optimizado para una sola mano en pantallas móviles.
- Panel de métricas en vivo en el chat (tokens/segundo, hardware activo y medidor de uso de contexto).
- Compatible con modo oscuro y paleta accesible de alto contraste.

---

## 🚀 Requisitos del Sistema
- **Android 8.0 (API 26)** o superior (Recomendado Android 12+ / API 31+ para telemetría completa de SoC).
- Arquitectura **arm64-v8a** (soporte 64-bit).
- Modelos recomendados para teléfonos móviles:
  - 0.5B a 1.5B (SmolLM-360M, Qwen2.5-0.5B, Llama-3.2-1B, Gemma-2-2B): ~0.5 GB a 2.5 GB RAM.
  - 3B a 8B (Llama-3.2-3B, Qwen2.5-7B Q4): dispositivos con 8 GB a 12 GB RAM.

---

## 🛠️ Estructura del Código

```
app/src/main/
├── cpp/                     # Motor nativo C++ (parser GGUF, decuantizador, streaming UTF-8)
├── rust/                    # Motor nativo Rust (Candle, SafeTensors, tokenizer)
└── java/com/example/
    ├── data/
    │   ├── local/           # Base de datos Room (LocalAiDatabase, ModelDao, ChatDao)
    │   │   ├── dao/         # DAOs para modelos y sesiones/mensajes
    │   │   └── entities/    # ModelEntity, ChatSessionEntity, ChatMessageEntity
    │   └── repository/      # ModelRepository y ChatRepository (Room SQLite)
    ├── engine/
    │   ├── hardware/        # HardwareCapabilityDetector (telemetría real de SoC, RAM, almacenamiento)
    │   ├── metrics/         # InferenceMetricsTracker (tokens/s, cálculo de contexto)
    │   └── LocalInferenceEngine.kt # Orquestador de streaming multi-backend
    ├── model/               # Modelos de datos (LocalAiModel, InferenceParameters, ChatMessage)
    ├── ui/
    │   ├── chat/            # Pantalla de conversación y streaming
    │   ├── dialogs/         # Historial de chats, selector de modelos, guía y parámetros
    │   ├── safetensors/     # Formulario de importación y edición modular SafeTensors
    │   ├── theme/           # Theming Material 3
    │   └── welcome/         # Pantalla principal limpia con telemetría, modelos guardados y chats
    └── viewmodel/           # ChatViewModel y ChatUiState
```

---

## 📦 Distribución, Descargas y Privacidad
- **Descargas Ultra Rápidas para Móviles (.7z):** En GitHub Actions, el APK se comprime automáticamente con **7-Zip Ultra (LZMA2 / `-mx=9` / Solid)** reduciendo drásticamente el peso del archivo para descargas veloces incluso con conexiones móviles lentas. Puedes descomprimirlo e instalarlo en Android con apps como *ZArchiver* o *RAR*.
- Diseñado para distribución directa mediante APK, Uptodown y GitHub Releases.
- Cero recolección de datos, cero telemetría externa, 100% privado en tu teléfono.

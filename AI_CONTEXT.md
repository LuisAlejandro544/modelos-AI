# 🧠 AI Context & Arquitectura del Proyecto

## Propósito del Proyecto
Esta aplicación es un cliente nativo de Inteligencia Artificial para Android que ejecuta modelos de lenguaje (LLMs / SLMs) en formatos `.gguf` y `.safetensors` de forma **100% local, offline y privada**, sin dependencias de servicios en la nube, sin llamadas simuladas a APIs externas y con control total del usuario sobre sus propios archivos.

---

## ⚡ Modos de Operación

1. **Modo GGUF (`.gguf` - Motor Nativo C++ llama.cpp):**
   - Selección de un único archivo `.gguf` que contiene pesos, vocabulario y metadatos de arquitectura en un solo binario.
   - **Parser de Cabeceras GGUF v2/v3 en C++:** Decodificación zero-copy de número mágico `0x46554747`, tensores, kv-metadata y plantillas de chat.
   - **Gestión Nativa de Memoria (`mmap`):** Mapeo virtual del modelo desde descriptores de archivo Android (`ParcelFileDescriptor`) sin duplicación en RAM de la JVM.
   - Soporte multihilo para arquitecturas ARM64 con aceleración NEON y Vulkan GPU.

2. **Modo SafeTensors (`.safetensors` - Inferencia Real con Hugging Face Candle):**
   - Pantalla dedicada para seleccionar por separado los 4 archivos obligatorios:
     1. Pesos: `*.safetensors` (tensores binarios deserializados con `mmap` zero-copy).
     2. Tokenizador: `tokenizer.json` (conversión real de texto a IDs de tokens con el crate `tokenizers` en Rust).
     3. Configuración de modelo: `config.json` (capas, dimensiones de embedding, cabezas de atención).
     4. Configuración del Tokenizador: `tokenizer_config.json` (Plantillas ChatML, Llama-3, Gemma, tokens especiales).
     5. Archivo auxiliar opcional: `generation_config.json`.
   - **Forward Pass Real en Rust (`lib.rs`):** Indexación de tensores de embedding, normalización `rms_norm`, multiplicación matricial hacia `lm_head` y muestreo probabilístico con `LogitsProcessor` (temperatura, Top-P).
   - Extracción reactiva de metadatos JSON al seleccionar archivos.

---

## ⚡ Componentes Clave

1. **Gestión de Ventana de Contexto y Tokens:**
   - Cálculo continuo de tokens estimados de la conversación (`approximateConversationTokens`).
   - Medidor superior con porcentaje de contexto utilizado y advertencias visuales al acercarse al límite (`contextLimit`).

2. **Monitoreo de Rendimiento (Tokens/Segundo):**
   - Cálculo en tiempo real de tokens por segundo generados durante el streaming (`liveTokensPerSec`).
   - Métricas detalladas al finalizar cada turno: acelerador, milisegundos de cómputo, t/s y uso de RAM/mmap.

3. **Selector de Aceleración de Hardware con Fallback:**
   - Modos `AUTO` (recomendado), `GPU` (Vulkan), `NPU` (NNAPI), y `CPU` (ARM NEON).
   - Conmutación transparente a GPU (Vulkan) si no existe NPU dedicada.

4. **Mapeo de Memoria (`mmap`):**
   - Paginación bajo demanda desde flash para optimizar RAM en teléfonos móviles.

---

## 🛠️ Tecnologías y Estándares
- **Lenguaje:** Kotlin (Coroutines y Flow).
- **UI:** Jetpack Compose con Material Design 3 (M3).
- **Patrón:** MVVM con `StateFlow` y `collectAsStateWithLifecycle`.
- **C++ NDK:** `local_ai_engine.cpp`, `gguf_parser.cpp`, `gguf_types.h` para `llama.cpp` y GGUF v2/v3.
- **Rust NDK:** `local_ai_rust` (`Candle 0.8.2` con `safetensors`, `tokenizers` y `memmap2`).
- **CI/CD:** GitHub Actions con soporte multi-ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
- **Testing:** Robolectric para tests locales en JVM y Roborazzi para capturas.

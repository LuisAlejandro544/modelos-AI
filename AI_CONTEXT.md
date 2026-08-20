# 🧠 AI Context & Arquitectura del Proyecto

## Propósito del Proyecto
Esta aplicación es un cliente nativo de Inteligencia Artificial para Android que ejecuta modelos de lenguaje (LLMs / SLMs) en formatos `.gguf` y `.safetensors` de forma **100% local, offline y privada**, sin dependencias de servicios en la nube, sin llamadas simuladas a Gemini por detrás y con control total del usuario sobre sus propios archivos.

---

## ⚡ Modos de Operación

1. **Modo GGUF (`.gguf`):**
   - Selección de un único archivo `.gguf` que contiene pesos, vocabulario y metadatos de arquitectura en un solo binario.
   - Enrutado al motor nativo C++ (`llama.cpp`) con soporte para GPU Vulkan y `mmap`.

2. **Modo SafeTensors (`.safetensors`):**
   - Pantalla dedicada para seleccionar por separado los 4 archivos obligatorios:
     1. Pesos: `*.safetensors`
     2. Tokenizador: `tokenizer.json` (conversión de texto a IDs de tokens)
     3. Configuración de modelo: `config.json` (capas, dimensiones, cabezas de atención)
     4. Configuración del Tokenizador: `tokenizer_config.json` (Plantillas ChatML, Llama-3, Gemma, tokens especiales)
     5. Archivo auxiliar opcional: `generation_config.json`.
   - Extracción reactiva de metadatos JSON al seleccionar archivos.
   - Enrutado al motor nativo Rust (`Candle`) con decodificación de tokens, plantillas de chat y seguridad de memoria.

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
- **C++ NDK:** `local_ai_engine.cpp` para `llama.cpp`.
- **Rust NDK:** `local_ai_rust` (`Candle`).
- **Testing:** Robolectric para tests locales en JVM y Roborazzi para capturas.

# 🧠 AI Context & Arquitectura del Proyecto

## Propósito del Proyecto
Esta aplicación es un cliente nativo de Inteligencia Artificial para Android que ejecuta modelos de lenguaje (LLMs / SLMs) en formatos `.gguf` y `.safetensors` de forma **100% local, offline y privada**, sin dependencias de servicios en la nube ni telemetría.

---

## ⚡ Componentes Clave Recientes

1. **Gestión de Ventana de Contexto y Tokens:**
   - La aplicación calcula continuamente los tokens estimados de la conversación (`approximateConversationTokens`) sumando el prompt del sistema y todos los mensajes (entradas y salidas).
   - Se muestra un medidor superior con el porcentaje de contexto utilizado y advertencias visuales cuando se acerca al límite (`contextLimit`).

2. **Monitoreo de Rendimiento (Tokens/Segundo):**
   - Durante la generación vía Flow, cada chunk emitido calcula en tiempo real los tokens por segundo generados (`liveTokensPerSec`).
   - Al finalizar, se registran métricas completas en `InferenceMetrics`: acelerador utilizado, tiempo de cómputo en milisegundos, tasa t/s media, memoria RAM residente y estado de `mmap`.

3. **Selector de Aceleración de Hardware con Fallback:**
   - Opciones: `AUTO` (recomendado), `GPU` (Vulkan / Adreno & Mali), `NPU` (NNAPI / Qualcomm QNN), y `CPU` (ARM NEON).
   - **Regla de negocio:** Si el usuario elige `AUTO` o `NPU` pero el dispositivo no tiene NPU dedicada, conmuta de forma transparente e instantánea a `GPU (Vulkan)` para asegurar la máxima velocidad sin fallos.

4. **Mapeo de Memoria (`mmap`):**
   - Activado por defecto en `InferenceParameters`.
   - Permite paginación bajo demanda desde almacenamiento flash, reduciendo el consumo de RAM física residente hasta en un 65% (ideal para dispositivos móviles con 3-4 GB de RAM).

---

## 🛠️ Tecnologías y Estándares
- **Lenguaje:** Kotlin (100% Coroutines y Flow).
- **UI:** Jetpack Compose con Material Design 3 (M3).
- **Patrón:** MVVM con `StateFlow` y `collectAsStateWithLifecycle`.
- **C++ NDK:** `local_ai_engine.cpp` para `llama.cpp`, vectorización ARM NEON, aceleración Vulkan y llamadas `mmap`.
- **Rust NDK:** `local_ai_rust` (`Candle`) para tensores con seguridad de memoria estricta.
- **Testing:** Robolectric para tests unitarios locales en JVM y Roborazzi para pruebas de interfaz y capturas.

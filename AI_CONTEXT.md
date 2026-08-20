# AI_CONTEXT.md - Contexto de Inteligencia Artificial Local

## 📱 Visión General del Proyecto
Esta aplicación es un entorno de inferencia de Inteligencia Artificial para Android **100% local, offline y privado**. Permite a los usuarios importar y ejecutar sus propios modelos LLM en formatos **GGUF** y **SafeTensors**, sin enviar telemetría ni depender de servidores en la nube.

---

## 🏗️ Arquitectura de Motores de Inferencia y Persistencia

```
+-------------------------------------------------------------------------+
|                         Jetpack Compose UI M3                           |
|  - WelcomeScreen (Header limpio, Telemetría Hardware, Modelos, Chats)   |
|  - SafeTensorsImportScreen (Carga y edición de archivos auxiliares)      |
|  - ChatScreen (Streaming reactivo, tokens/segundo, métricas y topbar)   |
|  - ChatHistoryDialog (Gestión de sesiones, reanudar, renombrar y borrar)|
+-------------------------------------------------------------------------+
                                    |
                                    v
+-------------------------------------------------------------------------+
|            ChatViewModel, ModelRepository & ChatRepository              |
|  - Persistencia de modelos en Room (LocalAiDatabase / ModelEntity)      |
|  - Historial completo de chats en Room (ChatSessionEntity, ChatMessage) |
|  - Inicio automático de chat desde 0 al importar nuevos modelos         |
|  - Telemetría real de hardware (HardwareCapabilityDetector)             |
|  - Sanitización de parámetros (InferenceParameters.sanitize)            |
+-------------------------------------------------------------------------+
              |                                            |
              v                                            v
+-----------------------------+              +-----------------------------+
|    C++ Engine (Llama.cpp)   |              |     Rust Engine (Candle)    |
| - Parser GGUF nativo        |              | - Parser SafeTensors        |
| - BPE / WordPiece Tokenizer |              | - Tokenizer JSON modular    |
| - Decuantización Q4/Q8      |              | - ModelConfig / ChatTemplate|
| - Streaming UTF-8 seguro    |              | - JNI Bridge bidireccional  |
+-----------------------------+              +-----------------------------+
```

---

## 💬 Administrador e Historial de Chats (Room Database)
1. **Inicio Limpio al Importar Modelo:** Al cargar o importar un nuevo modelo (`.gguf` o `.safetensors`), el sistema inicializa automáticamente una sesión de chat limpia desde 0 (`messages = emptyList()`), vinculada al nuevo modelo en Room.
2. **Historial Completo de Conversaciones:**
   - Entidad `ChatSessionEntity`: almacena identificador, título, modelo asociado, prompt de sistema, contador de mensajes, fragmento previo y fechas de actualización.
   - Entidad `ChatMessageEntity`: almacena cada mensaje (usuario o asistente), tokens generados, velocidad (tok/s), latencia y hardware utilizado.
3. **Gestión de Sesiones:** Diálogo interactivo accesible desde la pantalla de bienvenida y el chat para:
   - Iniciar una nueva conversación en cualquier momento (`+ Iniciar Nueva Conversación`).
   - Reanudar conversaciones previas con todo su historial y métricas guardadas.
   - Renombrar títulos de conversaciones.
   - Eliminar conversaciones y sus mensajes de manera atómica en SQLite.

---

## 🛠️ Telemetría de Hardware Real del Dispositivo
El módulo `HardwareCapabilityDetector` lee dinámicamente las capacidades reales del teléfono Android:
- **Dispositivo:** `Build.MANUFACTURER` + `Build.MODEL`
- **SoC:** `Build.SOC_MODEL` (API 31+) o detección basada en `Build.HARDWARE` / `Build.BOARD` (Qualcomm Snapdragon, MediaTek Dimensity, Google Tensor, Samsung Exynos).
- **Memoria RAM Real:** `ActivityManager.MemoryInfo` (`availMem` y `totalMem` en GB).
- **Almacenamiento:** `StatFs` para calcular espacio libre disponible en el almacenamiento interno para modelos de IA.
- **CPU & Arquitectura:** Núcleos disponibles vía `Runtime.getRuntime().availableProcessors()` y ABI principal (`arm64-v8a`).
- **Aceleradores:** GPU Vulkan / ARM NEON SIMD / NPU NNAPI.

---

## 💾 Persistencia y Edición de Modelos
1. **Modelos GGUF:**
   - Carga con 1 solo archivo.
   - Detección automática de arquitectura, capas, longitud de contexto y tensores.
   - Guardado automático en base de datos Room (`ModelEntity`).
2. **Modelos SafeTensors:**
   - Configuración modular de tensores (`.safetensors`), tokenizador (`tokenizer.json`), configuración de arquitectura (`config.json`), plantilla de chat (`tokenizer_config.json`) y generación (`generation_config.json`).
   - **Capacidad de edición y complementación:** El usuario puede modificar cualquier modelo SafeTensors existente para completar archivos faltantes, actualizar el prompt del sistema o reasignar rutas.
   - Persistencia completa en Room.

---

## 🔒 Privacidad y Reglas de Distribución Móvil
- Sin llamadas a internet para inferencia o telemetría.
- Distribución pensada para APK directo, Uptodown y GitHub Releases.
- Nunca se modifican propiedades restringidas del sistema como `persist.sys.*`.

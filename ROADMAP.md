# 🗺️ Roadmap de Desarrollo

Este documento detalla el estado actual del desarrollo y las metas futuras para la aplicación de IA Local en Android.

---

## 🟢 Fase 1: Fundamentos y UI Reactiva (Completada)
- [x] Interfaz de chat moderna y fluida con **Jetpack Compose** y **Material 3**.
- [x] Catálogo de modelos preconfigurados (Gemma 2, SmolLM, Qwen 2.5, Llama 3.2, Phi-3, DeepSeek-R1).
- [x] Selector e importador de archivos locales `.gguf` y `.safetensors`.
- [x] Diálogo interactivo de parámetros (Temperatura, Top-P, Repeat Penalty, Hilos de CPU).
- [x] Pruebas automatizadas unitarias, de integración y de interfaz con **Robolectric** y **Roborazzi**.

---

## 🟢 Fase 2: Rendimiento, Contexto y Aceleración Móvil (Completada)
- [x] **Contador de Tokens y Medidor de Contexto:** Monitoreo en tiempo real del tamaño de la conversación vs. el límite de la ventana de contexto del modelo.
- [x] **Medidor de Velocidad de Tokens por Segundo (t/s):** Contador en vivo durante el streaming y estadísticas de rendimiento post-generación.
- [x] **Acelerador de Hardware Seleccionable (GPU / NPU / CPU):**
  - [x] Soporte para aceleración por GPU (Vulkan / Adreno-Mali).
  - [x] Soporte para NPU (NNAPI / Qualcomm QNN).
  - [x] Conmutación / fallback automático a GPU si el dispositivo no cuenta con NPU física.
  - [x] Modo CPU clásico (ARM NEON multihilo).
- [x] **Mapeo de Memoria Optimizado (`mmap`):** Carga perezosa desde memoria flash para reducir drásticamente el uso de RAM física en teléfonos de 3-4 GB de RAM.

---

## 🟡 Fase 3: Persistencia y Gestión Avanzada de Chats (Próxima)
- [ ] Base de datos local **Room** para guardar y reanudar múltiples conversaciones independientes.
- [ ] Exportación de chats a formato Markdown (`.md`) y texto plano.
- [ ] Búsqueda y filtrado de mensajes históricos.
- [ ] Gestión inteligente de KV-Cache para conversaciones largas (truncado o resumen automático).

---

## 🟣 Fase 4: Capacidades Multimodales y RAG Local
- [ ] **RAG Local (Chat con Documentos):** Procesamiento e indexación local de archivos PDF/TXT para responder preguntas sobre documentos sin internet.
- [ ] **Visión Local (VLM):** Soporte para modelos ligeros de visión (ej. Moondream / Llama 3.2 Vision) usando la cámara del teléfono.
- [ ] **Transferencia Wi-Fi Local:** Servidor embebido para transferir modelos GGUF pesados desde una computadora al teléfono sin cables.

# 📱 Local AI Android (100% Offline & Private)

Aplicación nativa para Android desarrollada con **Kotlin**, **Jetpack Compose (Material Design 3)**, y motores nativos híbridos en **C++ (`llama.cpp` / NDK)** y **Rust (`Candle` / UniFFI)** para ejecutar modelos de Inteligencia Artificial locales sin conexión a internet, sin servidores externos y con privacidad garantizada.

---

## ⚡ Nuevas Capacidades y Optimizaciones

- 📊 **Contador de Tokens y Medidor de Ventana de Contexto:** Visualiza en tiempo real cuántos tokens lleva acumulada la conversación respecto al límite total del modelo (ej. `342 / 4,096 tokens (8.3%)`) con barra de progreso reactiva.
- ⚡ **Velocímetro de Tokens por Segundo (t/s) en Vivo:** Medición continua del rendimiento de generación tanto en tiempo real durante el streaming como en el resumen de métricas final de cada respuesta.
- 🚀 **Selector de Acelerador de Hardware (GPU / NPU / CPU):**
  - **Automático / GPU (Vulkan):** Si el teléfono no cuenta con coprocesador NPU dedicado, el sistema conmuta automáticamente a la GPU móvil (**Vulkan / Adreno & Mali**) para máxima tasa de tokens/segundo.
  - **NPU (NNAPI / Qualcomm QNN):** Inferencia en redes neuronales dedicadas de ultra bajo consumo térmico y de batería.
  - **CPU (ARM NEON Multihilo):** Inferencia con cálculo vectorial en los núcleos de CPU asignados por el usuario.
- 🧠 **Mapeo de Memoria Optimizado (`mmap`):** Carga perezosa de los pesos del modelo directamente desde el almacenamiento flash a la memoria virtual sin duplicar en la RAM física. Reduce el consumo de RAM hasta un **65%**, permitiendo ejecutar modelos de mayor tamaño en dispositivos con 3 GB o 4 GB de RAM.
- 📂 **Importación de Modelos Propios:** Soporta archivos `.gguf` y `.safetensors` desde el almacenamiento del teléfono.

---

## 🏗️ Arquitectura Técnica

```
                    ┌────────────────────────────────────────────────┐
                    │      Jetpack Compose UI (Material 3)           │
                    │  (ChatScreen, Context Meter, Parameters, etc.) │
                    └───────────────────────┬────────────────────────┘
                                            │ StateFlow / Coroutines
                                            ▼
                    ┌────────────────────────────────────────────────┐
                    │       ChatViewModel & Inference Manager        │
                    │  (Token Counter, Live t/s, Hardware Fallback)  │
                    └───────────────────────┬────────────────────────┘
                                            │
                     ┌──────────────────────┴──────────────────────┐
                     ▼                                             ▼
       ┌───────────────────────────┐                 ┌───────────────────────────┐
       │   C++ Engine (llama.cpp)  │                 │    Rust Engine (Candle)   │
       │  • Vulkan / GPU / NEON    │                 │  • Safe Tensors & Memory  │
       │  • mmap Flash Mapping     │                 │  • UniFFI / JNI Bridge    │
       └───────────────────────────┘                 └───────────────────────────┘
```

---

## 📦 Modelos Preconfigurados

| Modelo | Desarrollador | Parámetros | Cuantización | Memoria RAM (con mmap) | Velocidad Estimada |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Gemma 2 2B Instruct** | Google | 2.6B | Q4_K_M | ~600 MB RAM | ~25-35 tok/s (GPU) |
| **SmolLM 360M Instruct** | Hugging Face | 360M | Q4_K_M | ~120 MB RAM | ~48-65 tok/s (GPU) |
| **Qwen 2.5 0.5B Chat** | Alibaba Cloud | 0.5B | Q4_K_M | ~160 MB RAM | ~40-55 tok/s (GPU) |
| **Llama 3.2 1B Instruct** | Meta | 1.2B | Q4_K_M | ~320 MB RAM | ~30-42 tok/s (GPU) |
| **Phi-3 Mini 3.8B** | Microsoft | 3.8B | Q4_K_M | ~850 MB RAM | ~16-24 tok/s (GPU) |
| **DeepSeek-R1 Distill 1.5B**| DeepSeek AI | 1.5B | Q4_K_M | ~420 MB RAM | ~22-32 tok/s (GPU) |

---

## ⚙️ Parámetros Configurables

- **Acelerador:** Automático (NPU con fallback a GPU), GPU Vulkan, NPU NNAPI, CPU ARM NEON.
- **Mapeo mmap:** Activado / Desactivado.
- **Ventana de Contexto:** 512 a 8,192 tokens.
- **Hilos de CPU:** 1 a N núcleos.
- **Temperatura:** 0.0 a 1.5.
- **Top-P:** 0.1 a 1.0.
- **Max Tokens:** 64 a 2,048 tokens por respuesta.
- **Prompt de Sistema:** Personalizable por sesión o por modelo.

---

## 🚀 Distribución

- Preparado para distribución universal mediante **APK firmado** en tiendas de terceros como Uptodown, F-Droid o GitHub Releases.
- No requiere Google Play Services ni permisos invasivos.
- Flujo CI/CD automatizado con GitHub Actions en `.github/workflows/build-apk.yml`.

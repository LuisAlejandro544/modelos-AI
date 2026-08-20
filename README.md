# 📱 Local AI Android (100% Offline & Private)

Aplicación nativa para Android desarrollada con **Kotlin**, **Jetpack Compose (Material Design 3)**, y motores nativos híbridos en **C++ (`llama.cpp` / NDK)** y **Rust (`Candle` / UniFFI)** para ejecutar modelos de Inteligencia Artificial locales sin conexión a internet, sin servidores externos, sin simulaciones y con privacidad garantizada.

---

## ⚡ Modos de Importación Local

La aplicación cuenta con dos modos principales para cargar modelos almacenados en el teléfono:

1. ⚡ **Modo GGUF (`.gguf`):**
   - Carga directa con 1 solo archivo autocontenido.
   - Utiliza el motor C++ (`llama.cpp`) con aceleración por GPU (Vulkan) y mapeo flash `mmap`.
   - Selecciona el archivo `.gguf` desde el almacenamiento y la conversación se inicia de inmediato.

2. 🧩 **Modo SafeTensors (`.safetensors`):**
   - Carga modular en el motor nativo de Rust (`Candle`).
   - Pantalla de configuración dedicada para seleccionar los archivos necesarios por separado:
     - **Pesos:** `*.safetensors` (Obligatorio).
     - **Tokenizador:** `tokenizer.json` (Obligatorio).
     - **Configuración:** `config.json` (Obligatorio).
     - **Tokenizer Config:** `tokenizer_config.json` (Opcional).
     - **Generation Config:** `generation_config.json` (Opcional).
   - Permite personalizar el prompt inicial del sistema y definir parámetros antes de iniciar el chat.

---

## 🚀 Capacidades y Optimizaciones Móviles

- 📊 **Contador de Tokens y Medidor de Ventana de Contexto:** Visualiza en tiempo real cuántos tokens lleva acumulada la conversación respecto al límite total del modelo (ej. `342 / 4,096 tokens (8.3%)`) con barra de progreso reactiva.
- ⚡ **Velocímetro de Tokens por Segundo (t/s) en Vivo:** Medición continua del rendimiento de generación tanto en tiempo real durante el streaming como en el resumen de métricas final de cada respuesta.
- 🚀 **Selector de Acelerador de Hardware (GPU / NPU / CPU):**
  - **Automático / GPU (Vulkan):** Si el teléfono no cuenta con coprocesador NPU dedicado, el sistema conmuta automáticamente a la GPU móvil (**Vulkan / Adreno & Mali**) para máxima tasa de tokens/segundo.
  - **NPU (NNAPI / Qualcomm QNN):** Inferencia en redes neuronales dedicadas de ultra bajo consumo térmico y de batería.
  - **CPU (ARM NEON Multihilo):** Inferencia con cálculo vectorial en los núcleos de CPU asignados por el usuario.
- 🧠 **Mapeo de Memoria Optimizado (`mmap`):** Carga perezosa de los pesos del modelo directamente desde el almacenamiento flash a la memoria virtual sin duplicar en la RAM física. Reduce el consumo de RAM hasta un **65%**, permitiendo ejecutar modelos de mayor tamaño en dispositivos con 3 GB o 4 GB de RAM.

---

## 🏗️ Arquitectura Técnica

```
                    ┌────────────────────────────────────────────────┐
                    │      Jetpack Compose UI (Material 3)           │
                    │   (WelcomeScreen, SafeTensorsImport, Chat)     │
                    └───────────────────────┬────────────────────────┘
                                            │ StateFlow / Coroutines
                                            ▼
                    ┌────────────────────────────────────────────────┐
                    │       ChatViewModel & Inference Manager        │
                    │    (GGUF Direct & SafeTensors Bundle Flows)    │
                    └───────────────────────┬────────────────────────┘
                                            │
                                            ├────────────────────────────────┐
                                            ▼                                ▼
                              ┌───────────────────────────┐    ┌───────────────────────────┐
                              │   C++ Engine (llama.cpp)  │    │    Rust Engine (Candle)   │
                              │  • Vulkan / GPU / NEON    │    │  • Safe Tensors & Memory  │
                              │  • mmap Flash Mapping     │    │  • UniFFI / JNI Bridge    │
                              │  • Carga 1-click GGUF     │    │  • Carga modular multi-file│
                              └───────────────────────────┘    └───────────────────────────┘
```

---

## ⚙️ Parámetros Configurables

- **Acelerador:** Automático (NPU con fallback a GPU), GPU Vulkan, NPU NNAPI, CPU ARM NEON.
- **Mapeo mmap:** Activado / Desactivado.
- **Ventana de Contexto:** 512 a 8,192 tokens.
- **Hilos de CPU:** 1 a N núcleos.
- **Temperatura:** 0.0 a 1.5.
- **Top-P:** 0.1 a 1.0.
- **Max Tokens:** 64 a 2,048 tokens por respuesta.
- **Prompt de Sistema:** Personalizable por sesión o por modelo importado.

---

## 🚀 Distribución y APK

- Preparado para distribución universal mediante **APK firmado** en tiendas de terceros como Uptodown, F-Droid o GitHub Releases.
- No requiere Google Play Services ni permisos invasivos de red.
- Flujo CI/CD automatizado con GitHub Actions en `.github/workflows/build-apk.yml`.

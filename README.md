# AI Local — Inferencia de Modelos de Lenguaje en Android

**AI Local** es una aplicación nativa para Android desarrollada en **Kotlin** y **Jetpack Compose**, diseñada para ejecutar modelos de Inteligencia Artificial y LLMs (Large Language Models) **100% de forma local, offline y privada** en el hardware del propio dispositivo móvil (CPU ARM64, instrucciones NEON y aceleración nativa).

---

## 🚀 Características Principales

- **🔒 Privacidad y Modo Offline Total:** Ningún mensaje, prompt o dato personal sale del teléfono. Funciona sin internet ni llamadas a servidores externos.
- **📂 Soporte para Modelos Propios (GGUF & SafeTensors):**
  - **GGUF (`.gguf`):** Formato todo en uno con pesos cuantizados y tokenizador integrado.
  - **SafeTensors (`.safetensors`):** Carga directa de pesos de tensores crudos con configuración completa de archivos JSON (`tokenizer.json`, `config.json`, `tokenizer_config.json`, `generation_config.json`).
  - Soporte para modelos ultraligeros (**135M, 360M, 500M, 0.6B**) y modelos estándar (**1.1B, 1.5B, 2B, 3.8B**).
- **⚡ Motores de Inferencia Nativos:**
  - **C++ Engine (`llama.cpp` / NDK):** Inferencia de alto rendimiento compilada en C++17 con soporte para vectorización SIMD **ARM NEON**.
  - **Rust Engine (`Candle` / UniFFI):** Inferencia con seguridad de memoria garantizada en tiempo de compilación.
  - **Kotlin VM Engine:** Motor asíncrono basado en Corrutinas y Flows reactivos.
- **🎛️ Panel de Parámetros de Inferencia en Tiempo Real:**
  - Control de **Temperatura** (creatividad vs precisión).
  - Muestreo **Top-P (Nucleus)** y **Top-K**.
  - **Penalización por Repetición** (Repeat Penalty).
  - Selección de **Hilos de CPU Android** dedicados a la inferencia.
  - **Prompt de Sistema** editable por el usuario.
- **📊 Métricas de Generación por Mensaje:**
  - Velocidad de generación en **tokens por segundo (tok/s)**.
  - Tiempo de respuesta y latencia en milisegundos (ms).
  - Estimación de consumo de memoria RAM.
- **🎨 Interfaz Material Design 3:**
  - Paleta en tonos cálidos (pizarra, ámbar suave, azul marino), tipografía de alta legibilidad, soporte para pantallas pequeñas y tablets.

---

## 📦 Modelos Compatibles y Recomendados

| Modelo | Parámetros | Formato | RAM Estimada | Velocidad en Móvil | Uso Recomendado |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SmolLM 360M** | 360M | GGUF / SafeTensors | ~240 MB | 45-60 tok/s | Teléfonos básicos, velocidad instantánea |
| **Qwen 2.5 0.5B** | 500M | GGUF / SafeTensors | ~380 MB | 38-50 tok/s | Excelente español y seguimiento de órdenes |
| **Llama 3.2 1B** | 1.2B | GGUF | ~850 MB | 28-36 tok/s | Tareas generales, redacción y resúmenes |
| **Gemma 2 2B** | 2.6B | GGUF | ~1.6 GB | 18-25 tok/s | Razonamiento y redacción estructurada |
| **DeepSeek-R1 1.5B**| 1.5B | GGUF | ~1.1 GB | 20-26 tok/s | Razonamiento paso a paso |
| **Phi-3 Mini** | 3.8B | GGUF | ~2.3 GB | 12-16 tok/s | Lógica avanzada, matemáticas y código |

---

## 📚 Guía de Archivos de Modelos en Hugging Face

Al buscar modelos en Hugging Face, te encontrarás con diferentes archivos en la pestaña *Files and versions*. Esta es la función de cada uno:

| Archivo | Estado para SafeTensors | Estado para GGUF | ¿Para qué sirve? |
| :--- | :--- | :--- | :--- |
| **`model.safetensors`** | 🔴 **Obligatorio** | ❌ Innecesario | Contiene los pesos y matrices numéricas del modelo. |
| **`tokenizer.json`** | 🔴 **Obligatorio** | ❌ Ya embebido | El vocabulario para traducir texto a números (tokens) y viceversa. |
| **`config.json`** | 🔴 **Obligatorio** | ❌ Ya embebido | El plano arquitectónico: número de capas, dimensiones y cabezas. |
| **`tokenizer_config.json`** | 🟡 **Recomendado** | ❌ Ya embebido | Plantilla de chat (`chat_template`) y tokens de parada (`eos_token`). |
| **`generation_config.json`** | ⚪ **Opcional** | ❌ Ya embebido | Hiperparámetros de muestreo por defecto. |
| **`training_args.bin`** | ⛔ **IGNORAR** | ⛔ **IGNORAR** | Registros de entrenamiento antiguo. No sirve para inferencia. |

---

## 🛠️ Requisitos Técnicos e Instalación

### Requisitos del Sistema
- **Sistema Operativo:** Android 8.0 (API 26) o superior (Recomendado Android 12+ / API 31+).
- **Arquitectura:** ARM64-v8a / x86_64.
- **Memoria RAM:** Mínimo 3 GB de RAM (para modelos de 360M-1B); 6 GB+ de RAM (para modelos de 2B-3.8B).

### Compilación y Ejecución
```bash
# Compilar el APK en modo depuración
gradle assembleDebug

# Ejecutar la suite de tests unitarios y Robolectric
gradle :app:testDebugUnitTest
```

### GitHub Actions (CI/CD Automatizado)
El repositorio incluye un flujo en `.github/workflows/build-apk.yml` que:
1. Clona el código fuente.
2. Genera y firma el APK con `debug.keystore` en el entorno virtual de GitHub.
3. Almacena en caché las dependencias de Gradle para compilaciones instantáneas.
4. Genera el APK listo para descargar en la pestaña *Actions*.

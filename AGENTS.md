# Reglas y Directivas para Agentes de IA (AGENTS.md)

Este archivo contiene las directivas obligatorias para cualquier agente de IA o desarrollador que modifique esta base de código.

---

## 🛡️ Directivas Fundamentales

1. **Prioridad Absoluta al Modo Local y Privado:**
   - No sustituir la lógica local por APIs en la nube ni agregar servicios que envíen datos de usuario sin consentimiento explícito.
   - Preservar siempre la capacidad del usuario de importar sus propios archivos `.gguf` y `.safetensors`.

2. **Respeto a las Restricciones de Dispositivos Móviles:**
   - El usuario opera principalmente desde un teléfono móvil. La app está pensada para distribuirse vía APK / tiendas de terceros como Uptodown o GitHub Releases.
   - En caso de crear utilidades de rendimiento o Game Boosters, **nunca modificar propiedades `persist.sys.*`** que requieran privilegios de superusuario root o que pongan en riesgo la estabilidad del sistema operativo Android.

3. **Arquitectura y Estándares de Código:**
   - Usar **Kotlin** y **Jetpack Compose** para toda la interfaz gráfica.
   - Seguir estrictamente las pautas de diseño **Material Design 3 (M3)** sin usar colores estridentes o diseños genéricos artificiales.
   - Mantener las capas nativas de C++ y Rust desacopladas a través de sus correspondientes puentes en `/app/src/main/java/com/example/engine/`.
   - Utilizar identificadores `Modifier.testTag("...")` en todos los componentes interactivos principales para permitir pruebas automatizadas continuas con Robolectric y Roborazzi.

4. **Gestión de Formatos de Modelo:**
   - Si se añade soporte para nuevos formatos (como ONNX o TFLite), documentar claramente si el formato requiere un archivo de tokenizador independiente (`tokenizer.json` / `vocab.json`) o si está autocontenido como GGUF.

5. **Validación y Estabilidad de Parámetros de Inferencia:**
   - Respetar siempre los límites arquitectónicos del modelo cargado (`contextLength`, tokens, temperatura, top-p, hilos de CPU).
   - Toda interfaz o ViewModel debe invocar `InferenceParameters.sanitize()` para prevenir valores fuera de rango o desbordamientos que provoquen cierres inesperados del motor de inferencia nativo.
   - No aplicar restricciones artificiales basadas en estimaciones de RAM a menos que el usuario lo solicite explícitamente.

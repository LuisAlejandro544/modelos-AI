package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.HardwareAccelerator
import com.example.model.InferenceBackend
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel
import kotlin.math.roundToInt

@Composable
fun ParametersDialog(
  currentParameters: InferenceParameters,
  selectedModel: LocalAiModel? = null,
  maxAvailableCores: Int = 8,
  onSave: (InferenceParameters) -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit
) {
  val modelMaxContext = selectedModel?.contextLength ?: InferenceParameters.DEFAULT_MAX_CONTEXT_WINDOW
  val sanitizedParams = currentParameters.sanitize(selectedModel, maxAvailableCores)

  var selectedAccelerator by remember { mutableStateOf(sanitizedParams.accelerator) }
  var selectedBackend by remember { mutableStateOf(sanitizedParams.backend) }
  var useMmap by remember { mutableStateOf(sanitizedParams.useMmap) }
  var contextWindow by remember { mutableIntStateOf(sanitizedParams.contextWindow) }
  var temperature by remember { mutableFloatStateOf(sanitizedParams.temperature) }
  var topP by remember { mutableFloatStateOf(sanitizedParams.topP) }
  var topK by remember { mutableIntStateOf(sanitizedParams.topK) }
  var maxTokens by remember { mutableIntStateOf(sanitizedParams.maxTokens) }
  var repeatPenalty by remember { mutableFloatStateOf(sanitizedParams.repeatPenalty) }
  var cpuThreads by remember { mutableIntStateOf(sanitizedParams.cpuThreads) }
  var systemPrompt by remember { mutableStateOf(sanitizedParams.systemPrompt) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("parameters_dialog"),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth()
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Ajustes de Inferencia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Aceleración de hardware, mmap y contexto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_parameters_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cerrar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

          // 1. Hardware Accelerator Selection (GPU / NPU / CPU)
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Acelerador de Hardware (GPU / NPU / CPU):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Spacer(modifier = Modifier.height(6.dp))

            HardwareAccelerator.entries.forEach { accelerator ->
              val isSelected = accelerator == selectedAccelerator
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.5.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                  )
                  .clickable { selectedAccelerator = accelerator }
                  .testTag("accelerator_option_${accelerator.name}"),
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = when (accelerator) {
                          HardwareAccelerator.AUTO -> Icons.Default.Bolt
                          HardwareAccelerator.GPU -> Icons.Default.Speed
                          HardwareAccelerator.NPU -> Icons.Default.DeveloperBoard
                          HardwareAccelerator.CPU -> Icons.Default.Memory
                        },
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = accelerator.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Text(
                          text = accelerator.badge,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.sp
                        )
                      }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = accelerator.techDescription,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontSize = 11.5.sp
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }

          // 2. Mmap Optimization Toggle Card
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .border(
                width = if (useMmap) 1.dp else 0.5.dp,
                color = if (useMmap) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
              )
              .clickable { useMmap = !useMmap }
              .testTag("mmap_toggle_card"),
            colors = CardDefaults.cardColors(
              containerColor = if (useMmap) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Mapeo de memoria optimizado (mmap)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = "Carga perezosa de los pesos desde el almacenamiento flash a memoria virtual. Reduce drásticamente el uso de RAM física hasta un 65% en teléfonos de 3-4 GB.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.5.sp,
                  lineHeight = 16.sp
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Switch(
                checked = useMmap,
                onCheckedChange = { useMmap = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                  checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.testTag("mmap_switch")
              )
            }
          }

          // 3. Context Window Slider (Bounded to Model Max Architecture)
          ParameterCard(
            title = "Ventana de Contexto: $contextWindow tokens",
            description = if (selectedModel != null) {
              "Límite máximo soportado por ${selectedModel.name}: $modelMaxContext tokens."
            } else {
              "Límite de memoria de conversación para recordar mensajes previos."
            }
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Min: 256",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                  .padding(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "Límite nativo: $modelMaxContext tokens",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 10.sp
                )
              }
              Text(
                text = "Max: $modelMaxContext",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Slider(
              value = contextWindow.toFloat(),
              onValueChange = {
                val newContext = it.roundToInt()
                contextWindow = newContext
                if (maxTokens > newContext) {
                  maxTokens = newContext
                }
              },
              valueRange = 256f..modelMaxContext.toFloat(),
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("context_window_slider")
            )
          }

          // 4. Native Backend Selection
          Column {
            Text(
              text = "Framework de Inferencia Base:",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            InferenceBackend.entries.forEach { backend ->
              val isSelected = backend == selectedBackend
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                  )
                  .clickable { selectedBackend = backend }
                  .testTag("backend_option_${backend.name}"),
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = backend.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                          .padding(horizontal = 5.dp, vertical = 1.dp)
                      ) {
                        Text(
                          text = backend.badge,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.secondary,
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.sp
                        )
                      }
                    }
                    Text(
                      text = backend.techDescription,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontSize = 11.sp
                    )
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }

          // Temperature Slider (Safe clamped 0.0 - 2.0)
          ParameterCard(
            title = "Temperatura: ${((temperature * 100).roundToInt() / 100.0)}",
            description = when {
              temperature < 0.2f -> "Determinista: Respuestas muy directas y lógicas"
              temperature < 0.8f -> "Equilibrado: Respuestas naturales y coherentes (Recomendado)"
              temperature <= 1.2f -> "Creativo: Mayor expresividad y riqueza léxica"
              else -> "Muy alto: Alta aleatoriedad (Riesgo de respuestas incoherentes)"
            }
          ) {
            Slider(
              value = temperature,
              onValueChange = { temperature = it },
              valueRange = 0.0f..2.0f,
              steps = 19,
              modifier = Modifier.testTag("temperature_slider")
            )
          }

          // Max Tokens Slider (Clamped to Context Window)
          val maxAllowedTokens = contextWindow.coerceAtLeast(32)
          ParameterCard(
            title = "Longitud Máxima de Respuesta: $maxTokens tokens",
            description = "Tope de tokens por respuesta (Acotado por la ventana de contexto: máx $maxAllowedTokens tokens)"
          ) {
            Slider(
              value = maxTokens.toFloat(),
              onValueChange = { maxTokens = it.roundToInt().coerceAtMost(maxAllowedTokens) },
              valueRange = 32f..maxAllowedTokens.toFloat(),
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
              ),
              modifier = Modifier.testTag("max_tokens_slider")
            )
          }

          // Top-K Slider
          ParameterCard(
            title = "Top-K (Filtro de Vocabulario): $topK",
            description = "Limita la selección a los $topK tokens más probables del vocabulario"
          ) {
            Slider(
              value = topK.toFloat(),
              onValueChange = { topK = it.roundToInt() },
              valueRange = 1f..100f,
              steps = 98,
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("top_k_slider")
            )
          }

          // CPU Threads Slider
          ParameterCard(
            title = "Hilos de CPU Android: $cpuThreads núcleos",
            description = "Núcleos del procesador asignados al cálculo en modo CPU"
          ) {
            Slider(
              value = cpuThreads.toFloat(),
              onValueChange = { cpuThreads = it.roundToInt() },
              valueRange = 1f..maxAvailableCores.coerceAtLeast(1).toFloat(),
              steps = (maxAvailableCores.coerceAtLeast(1) - 2).coerceAtLeast(0),
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
              ),
              modifier = Modifier.testTag("cpu_threads_slider")
            )
          }

          // Top-P Slider
          ParameterCard(
            title = "Top-P (Nucleus Sampling): ${((topP * 100).roundToInt() / 100.0)}",
            description = "Filtra la masa de probabilidad acumulada de los tokens candidatos"
          ) {
            Slider(
              value = topP,
              onValueChange = { topP = it },
              valueRange = 0.01f..1.0f,
              steps = 19,
              modifier = Modifier.testTag("top_p_slider")
            )
          }

          // Repeat Penalty Slider
          ParameterCard(
            title = "Penalización por Repetición: ${((repeatPenalty * 100).roundToInt() / 100.0)}",
            description = "Reduce la probabilidad de que el modelo repita frases o bucles idénticos"
          ) {
            Slider(
              value = repeatPenalty,
              onValueChange = { repeatPenalty = it },
              valueRange = 1.0f..2.0f,
              steps = 19,
              modifier = Modifier.testTag("repeat_penalty_slider")
            )
          }

          // System Prompt Field
          Column {
            Text(
              text = "Prompt de Sistema (Instrucciones base):",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = systemPrompt,
              onValueChange = { systemPrompt = it },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("system_prompt_input"),
              minLines = 3,
              maxLines = 5,
              textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
              shape = RoundedCornerShape(12.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = {
              onReset()
              selectedAccelerator = HardwareAccelerator.AUTO
              selectedBackend = when (selectedModel?.formatType) {
                com.example.model.ModelFormatType.SAFETENSORS -> InferenceBackend.RUST_CANDLE
                com.example.model.ModelFormatType.TFLITE -> InferenceBackend.TFLITE_RUNTIME
                else -> InferenceBackend.CPP_LLAMA
              }
              useMmap = true
              contextWindow = modelMaxContext
              temperature = 0.7f
              topP = 0.90f
              topK = 40
              maxTokens = 512.coerceAtMost(modelMaxContext)
              repeatPenalty = 1.15f
              cpuThreads = 4.coerceAtMost(maxAvailableCores)
              systemPrompt = selectedModel?.defaultSystemPrompt ?: "Eres un asistente de IA local y privado ejecutado en este dispositivo Android."
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("reset_parameters_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restaurar")
          }

          Button(
            onClick = {
              val updated = currentParameters.copy(
                accelerator = selectedAccelerator,
                backend = selectedBackend,
                useMmap = useMmap,
                contextWindow = contextWindow,
                temperature = ((temperature * 100).roundToInt() / 100.0f),
                topP = ((topP * 100).roundToInt() / 100.0f),
                topK = topK,
                maxTokens = maxTokens,
                repeatPenalty = ((repeatPenalty * 100).roundToInt() / 100.0f),
                cpuThreads = cpuThreads,
                systemPrompt = systemPrompt
              ).sanitize(selectedModel, maxAvailableCores)
              onSave(updated)
            },
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("save_parameters_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            )
          ) {
            Text("Guardar Cambios", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun ParameterCard(
  title: String,
  description: String,
  content: @Composable () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.5.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      content()
    }
  }
}

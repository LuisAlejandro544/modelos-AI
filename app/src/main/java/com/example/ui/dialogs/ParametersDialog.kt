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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
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
import com.example.model.InferenceBackend
import com.example.model.InferenceParameters
import kotlin.math.roundToInt

@Composable
fun ParametersDialog(
  currentParameters: InferenceParameters,
  maxAvailableCores: Int = 8,
  onSave: (InferenceParameters) -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedBackend by remember { mutableStateOf(currentParameters.backend) }
  var temperature by remember { mutableFloatStateOf(currentParameters.temperature) }
  var topP by remember { mutableFloatStateOf(currentParameters.topP) }
  var topK by remember { mutableIntStateOf(currentParameters.topK) }
  var maxTokens by remember { mutableIntStateOf(currentParameters.maxTokens) }
  var repeatPenalty by remember { mutableFloatStateOf(currentParameters.repeatPenalty) }
  var cpuThreads by remember { mutableIntStateOf(currentParameters.cpuThreads) }
  var systemPrompt by remember { mutableStateOf(currentParameters.systemPrompt) }

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
                text = "Parámetros de Inferencia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Ajusta la ejecución local y motor nativo",
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
          // Backend Selection
          Column {
            Text(
              text = "Motor de Inferencia Nativo:",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            InferenceBackend.values().forEach { backend ->
              val isSelected = backend == selectedBackend
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                  )
                  .clickable { selectedBackend = backend }
                  .testTag("backend_option_${backend.name}"),
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
                          .padding(horizontal = 6.dp, vertical = 2.dp)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = backend.techDescription,
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

          // Temperature Slider
          ParameterCard(
            title = "Temperatura: ${((temperature * 100).roundToInt() / 100.0)}",
            description = when {
              temperature < 0.3f -> "Muy estricto, respuestas exactas y repetibles"
              temperature < 0.8f -> "Equilibrado, respuestas naturales y coherentes"
              else -> "Muy creativo, mayor variedad de vocabulario"
            }
          ) {
            Slider(
              value = temperature,
              onValueChange = { temperature = it },
              valueRange = 0.0f..1.5f,
              steps = 14,
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.testTag("temperature_slider")
            )
          }

          // Max Tokens Slider
          ParameterCard(
            title = "Longitud Máxima: $maxTokens tokens",
            description = "Límite de palabras generadas por respuesta (~${(maxTokens * 0.75).toInt()} palabras)"
          ) {
            Slider(
              value = maxTokens.toFloat(),
              onValueChange = { maxTokens = it.roundToInt() },
              valueRange = 64f..2048f,
              steps = 30,
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
              ),
              modifier = Modifier.testTag("max_tokens_slider")
            )
          }

          // CPU Threads Slider
          ParameterCard(
            title = "Hilos de CPU Android: $cpuThreads núcleos",
            description = "Núcleos del procesador asignados al cálculo de tensores locales"
          ) {
            Slider(
              value = cpuThreads.toFloat(),
              onValueChange = { cpuThreads = it.roundToInt() },
              valueRange = 1f..maxAvailableCores.coerceAtLeast(4).toFloat(),
              steps = (maxAvailableCores.coerceAtLeast(4) - 2).coerceAtLeast(0),
              colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.tertiary,
                activeTrackColor = MaterialTheme.colorScheme.tertiary
              ),
              modifier = Modifier.testTag("cpu_threads_slider")
            )
          }

          // Top-P Slider
          ParameterCard(
            title = "Top-P (Nucleus): ${((topP * 100).roundToInt() / 100.0)}",
            description = "Filtra la probabilidad acumulada de los tokens candidatos"
          ) {
            Slider(
              value = topP,
              onValueChange = { topP = it },
              valueRange = 0.1f..1.0f,
              steps = 17,
              modifier = Modifier.testTag("top_p_slider")
            )
          }

          // Repeat Penalty Slider
          ParameterCard(
            title = "Penalización por Repetición: ${((repeatPenalty * 100).roundToInt() / 100.0)}",
            description = "Reduce la probabilidad de que el modelo repita frases idénticas"
          ) {
            Slider(
              value = repeatPenalty,
              onValueChange = { repeatPenalty = it },
              valueRange = 1.0f..1.5f,
              steps = 9,
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
              selectedBackend = InferenceBackend.CPP_LLAMA
              temperature = 0.7f
              topP = 0.90f
              topK = 40
              maxTokens = 512
              repeatPenalty = 1.15f
              cpuThreads = 4
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
                backend = selectedBackend,
                temperature = ((temperature * 100).roundToInt() / 100.0f),
                topP = ((topP * 100).roundToInt() / 100.0f),
                topK = topK,
                maxTokens = maxTokens,
                repeatPenalty = ((repeatPenalty * 100).roundToInt() / 100.0f),
                cpuThreads = cpuThreads,
                systemPrompt = systemPrompt
              )
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

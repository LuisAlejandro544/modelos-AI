package com.example.ui.safetensors.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SafeTensorsMetadataForm(
  modelName: String,
  onModelNameChange: (String) -> Unit,
  paramSize: String,
  onParamSizeChange: (String) -> Unit,
  quantization: String,
  onQuantizationChange: (String) -> Unit,
  customPrompt: String,
  onCustomPromptChange: (String) -> Unit,
  areRequiredFilesSelected: Boolean,
  onStartInference: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Text(
      text = "3. Metadatos del Modelo (Autocompletados o Personalizados)",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    OutlinedTextField(
      value = modelName,
      onValueChange = onModelNameChange,
      label = { Text("Nombre del Modelo") },
      placeholder = { Text("Ej: SmolLM 360M SafeTensors") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("safetensors_name_input"),
      shape = RoundedCornerShape(12.dp),
      singleLine = true
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedTextField(
        value = paramSize,
        onValueChange = onParamSizeChange,
        label = { Text("Parámetros") },
        placeholder = { Text("0.5B / 1.5B") },
        modifier = Modifier
          .weight(1f)
          .testTag("safetensors_params_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      OutlinedTextField(
        value = quantization,
        onValueChange = onQuantizationChange,
        label = { Text("Precisión") },
        placeholder = { Text("F16 / BF16") },
        modifier = Modifier
          .weight(1f)
          .testTag("safetensors_quant_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )
    }

    OutlinedTextField(
      value = customPrompt,
      onValueChange = onCustomPromptChange,
      label = { Text("Prompt de Sistema Inicial") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("safetensors_prompt_input"),
      shape = RoundedCornerShape(12.dp),
      minLines = 2,
      maxLines = 4
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Final Action: Start Conversation
    Button(
      onClick = onStartInference,
      enabled = areRequiredFilesSelected,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("safetensors_start_chat_button"),
      shape = RoundedCornerShape(16.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary
      )
    ) {
      Text(
        text = if (areRequiredFilesSelected) "Iniciar Inferencia SafeTensors" else "Selecciona los 4 archivos obligatorios",
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp
      )
      if (areRequiredFilesSelected) {
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

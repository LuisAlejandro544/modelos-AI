package com.example.ui.dialogs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.ModelFormatType

@Composable
fun ImportModelDialog(
  onImport: (
    name: String,
    formatType: ModelFormatType,
    parameterSize: String,
    quantization: String,
    fileUriOrPath: String,
    tokenizerUriOrPath: String?,
    customPrompt: String
  ) -> Unit,
  onOpenTokenizerGuide: () -> Unit,
  onDismiss: () -> Unit
) {
  var modelName by remember { mutableStateOf("") }
  var selectedFormat by remember { mutableStateOf(ModelFormatType.GGUF) }
  var parameterSize by remember { mutableStateOf("500M") }
  var quantization by remember { mutableStateOf("Q4_K_M") }
  var modelFilePath by remember { mutableStateOf("") }
  var tokenizerFilePath by remember { mutableStateOf("") }
  var systemPrompt by remember { mutableStateOf("Eres un asistente inteligente ejecutado desde un archivo de modelo propio.") }

  // File Picker Launcher for Model Weights
  val modelFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let {
      val lastPathSegment = it.lastPathSegment ?: it.toString()
      modelFilePath = it.toString()
      if (modelName.isBlank()) {
        val cleanName = lastPathSegment.substringAfterLast("/").substringBeforeLast(".")
        modelName = cleanName.replace("-", " ").replace("_", " ").capitalizeWords()
      }
      if (lastPathSegment.endsWith(".safetensors", ignoreCase = true)) {
        selectedFormat = ModelFormatType.SAFETENSORS
      } else if (lastPathSegment.endsWith(".gguf", ignoreCase = true)) {
        selectedFormat = ModelFormatType.GGUF
      }
    }
  }

  // File Picker Launcher for Tokenizer JSON
  val tokenizerPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let {
      tokenizerFilePath = it.toString()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("import_model_dialog"),
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
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Importar Modelo Propio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Carga archivos .gguf o .safetensors locales",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_import_dialog_button")
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
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Format selection
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Formato de archivo:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )

              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onOpenTokenizerGuide)
                  .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.HelpOutline,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "¿Cuál elegir?",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ModelFormatType.values().forEach { format ->
                val isSelected = format == selectedFormat
                Card(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                      width = if (isSelected) 1.5.dp else 0.5.dp,
                      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { selectedFormat = format }
                    .testTag("format_option_${format.name}"),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                  )
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                      text = format.displayName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = if (format == ModelFormatType.GGUF) "Pesos + Tokenizador" else "Pesos + tokenizer.json",
                      style = MaterialTheme.typography.bodySmall,
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          // Model Name Field
          Column {
            Text(
              text = "Nombre del Modelo:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = modelName,
              onValueChange = { modelName = it },
              placeholder = { Text("Ej: SmolLM 360M Custom / Qwen2.5 0.5B") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_model_name_input"),
              singleLine = true,
              shape = RoundedCornerShape(12.dp)
            )
          }

          // Model File Selector
          Column {
            Text(
              text = "Archivo de Pesos (${selectedFormat.extension}):",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = if (modelFilePath.isNotBlank()) modelFilePath.substringAfterLast("/") else "",
                onValueChange = { modelFilePath = it },
                placeholder = { Text("Selecciona archivo ${selectedFormat.extension}") },
                modifier = Modifier
                  .weight(1f)
                  .testTag("custom_model_path_input"),
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
              )
              Button(
                onClick = {
                  modelFilePickerLauncher.launch(arrayOf("*/*"))
                },
                modifier = Modifier.testTag("browse_model_file_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.FolderOpen,
                  contentDescription = "Explorar",
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Elegir")
              }
            }
          }

          // Tokenizer File Selector (If SafeTensors)
          if (selectedFormat == ModelFormatType.SAFETENSORS) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Archivo Tokenizer (tokenizer.json):",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Requerido",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = if (tokenizerFilePath.isNotBlank()) tokenizerFilePath.substringAfterLast("/") else "",
                  onValueChange = { tokenizerFilePath = it },
                  placeholder = { Text("tokenizer.json o vocab.json") },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("tokenizer_path_input"),
                  readOnly = true,
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp)
                )
                Button(
                  onClick = {
                    tokenizerPickerLauncher.launch(arrayOf("*/*"))
                  },
                  modifier = Modifier.testTag("browse_tokenizer_file_button"),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Explorar",
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Elegir")
                }
              }
            }
          }

          // Parameter Size Chips
          Column {
            Text(
              text = "Cantidad de Parámetros:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sizes = listOf("135M", "360M", "500M", "0.6B", "1.1B", "1.5B", "2B", "3B", "7B")
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              sizes.take(5).forEach { size ->
                FilterChip(
                  selected = parameterSize == size,
                  onClick = { parameterSize = size },
                  label = { Text(size, fontSize = 11.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                  )
                )
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              sizes.drop(5).forEach { size ->
                FilterChip(
                  selected = parameterSize == size,
                  onClick = { parameterSize = size },
                  label = { Text(size, fontSize = 11.5.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                  )
                )
              }
            }
          }

          // Quantization selector
          Column {
            Text(
              text = "Cuantización:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            val quants = listOf("Q4_K_M", "Q4_0", "Q5_K_M", "Q8_0", "F16")
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              quants.forEach { q ->
                FilterChip(
                  selected = quantization == q,
                  onClick = { quantization = q },
                  label = { Text(q, fontSize = 11.sp) }
                )
              }
            }
          }

          // System prompt
          Column {
            Text(
              text = "Prompt de Sistema Inicial:",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = systemPrompt,
              onValueChange = { systemPrompt = it },
              modifier = Modifier.fillMaxWidth(),
              minLines = 2,
              maxLines = 4,
              shape = RoundedCornerShape(12.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(48.dp),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Cancelar")
          }

          Button(
            onClick = {
              val finalName = if (modelName.isNotBlank()) modelName else "Modelo ${selectedFormat.name} Local"
              val finalPath = if (modelFilePath.isNotBlank()) modelFilePath else "/sdcard/Download/model${selectedFormat.extension}"
              val finalTokenizer = if (tokenizerFilePath.isNotBlank()) tokenizerFilePath else null

              onImport(
                finalName,
                selectedFormat,
                parameterSize,
                quantization,
                finalPath,
                finalTokenizer,
                systemPrompt
              )
            },
            modifier = Modifier
              .weight(1.4f)
              .height(48.dp)
              .testTag("confirm_import_model_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cargar Modelo", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

private fun String.capitalizeWords(): String {
  return split(" ").joinToString(" ") { word ->
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }
}

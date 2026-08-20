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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.GgufParsedInfo
import com.example.engine.NativeCppBridge
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
    configUriOrPath: String?,
    tokenizerConfigUriOrPath: String?,
    generationConfigUriOrPath: String?,
    customPrompt: String
  ) -> Unit,
  onOpenTokenizerGuide: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var modelName by remember { mutableStateOf("") }
  var selectedFormat by remember { mutableStateOf(ModelFormatType.GGUF) }
  var parameterSize by remember { mutableStateOf("500M") }
  var quantization by remember { mutableStateOf("Q4_K_M") }
  var detectedGgufInfo by remember { mutableStateOf<GgufParsedInfo?>(null) }
  
  // File Paths
  var modelFilePath by remember { mutableStateOf("") }
  var tokenizerFilePath by remember { mutableStateOf("") }
  var configFilePath by remember { mutableStateOf("") }
  var tokenizerConfigFilePath by remember { mutableStateOf("") }
  var generationConfigFilePath by remember { mutableStateOf("") }
  
  var systemPrompt by remember { mutableStateOf("Eres un asistente inteligente ejecutado desde un archivo de modelo propio.") }

  // File Picker Launchers
  val modelFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let {
      val uriStr = it.toString()
      val lastPathSegment = it.lastPathSegment ?: uriStr
      modelFilePath = uriStr

      if (lastPathSegment.endsWith(".safetensors", ignoreCase = true)) {
        selectedFormat = ModelFormatType.SAFETENSORS
        detectedGgufInfo = null
      } else if (lastPathSegment.endsWith(".tflite", ignoreCase = true) || lastPathSegment.endsWith(".task", ignoreCase = true)) {
        selectedFormat = ModelFormatType.TFLITE
        detectedGgufInfo = null
        if (modelName.isBlank()) {
          val cleanName = lastPathSegment.substringAfterLast("/").substringBeforeLast(".")
          modelName = "${cleanName.replace("-", " ").replace("_", " ").capitalizeWords()} (TFLite)"
        }
      } else {
        selectedFormat = ModelFormatType.GGUF
        val meta = NativeCppBridge.parseGgufMetadataSafe(uriStr, context)
        detectedGgufInfo = meta
        if (meta != null && meta.isValid) {
          if (modelName.isBlank() || modelName.startsWith("Modelo")) {
            modelName = if (meta.modelName.isNotBlank() && meta.modelName != "GGUF Model") {
              meta.modelName
            } else {
              "${meta.architecture.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }} GGUF (Local)"
            }
          }
          if (meta.blockCount > 0) {
            parameterSize = when {
              meta.blockCount <= 16 -> "500M"
              meta.blockCount <= 24 -> "1.1B"
              meta.blockCount <= 28 -> "1.5B"
              meta.blockCount <= 32 -> "3B"
              else -> "7B"
            }
          }
        } else if (modelName.isBlank()) {
          val cleanName = lastPathSegment.substringAfterLast("/").substringBeforeLast(".")
          modelName = cleanName.replace("-", " ").replace("_", " ").capitalizeWords()
        }
      }
    }
  }

  val tokenizerPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { tokenizerFilePath = it.toString() }
  }

  val configPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { configFilePath = it.toString() }
  }

  val tokenizerConfigPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { tokenizerConfigFilePath = it.toString() }
  }

  val generationConfigPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    uri?.let { generationConfigFilePath = it.toString() }
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
                text = "Carga .gguf o .safetensors con sus configs",
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
                  text = "Guía de Archivos",
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
              ModelFormatType.entries.forEach { format ->
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
                      text = when (format) {
                        ModelFormatType.GGUF -> "1 archivo todo en uno"
                        ModelFormatType.SAFETENSORS -> "Pesos + Configs JSON"
                        ModelFormatType.TFLITE -> "FlatBuffers + Tokenizer"
                      },
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

          // 1. Model Weights File
          FileSelectorField(
            title = "1. Archivo de Pesos (${selectedFormat.extension}):",
            badgeText = "Obligatorio",
            badgeColor = MaterialTheme.colorScheme.primary,
            filePath = modelFilePath,
            placeholder = "Selecciona archivo ${selectedFormat.extension}",
            testTag = "custom_model_path_input",
            buttonTestTag = "browse_model_file_button",
            onBrowseClick = { modelFilePickerLauncher.launch(arrayOf("*/*")) }
          )

          // GGUF detected metadata preview banner
          if (selectedFormat == ModelFormatType.GGUF && detectedGgufInfo != null && detectedGgufInfo?.isValid == true) {
            val meta = detectedGgufInfo!!
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
              ),
              shape = RoundedCornerShape(14.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "⚡ Cabecera GGUF v${meta.version} validada",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = meta.architecture.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "• Contexto nativo: ${meta.contextLength} tokens | Tensores: ${meta.tensorCount}",
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "• Mapeo flash: mmap zero-copy compatible con ParcelFileDescriptor",
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // SafeTensors specific configs
          if (selectedFormat == ModelFormatType.SAFETENSORS) {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              ),
              shape = RoundedCornerShape(14.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Text(
                  text = "Archivos de Configuración para SafeTensors:",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )

                // 2. tokenizer.json (Obligatorio)
                FileSelectorField(
                  title = "2. tokenizer.json (Vocabulario):",
                  badgeText = "Obligatorio",
                  badgeColor = MaterialTheme.colorScheme.error,
                  filePath = tokenizerFilePath,
                  placeholder = "tokenizer.json (conversión texto a IDs)",
                  testTag = "tokenizer_path_input",
                  buttonTestTag = "browse_tokenizer_file_button",
                  onBrowseClick = { tokenizerPickerLauncher.launch(arrayOf("*/*")) }
                )

                // 3. config.json (Obligatorio)
                FileSelectorField(
                  title = "3. config.json (Arquitectura y capas):",
                  badgeText = "Obligatorio",
                  badgeColor = MaterialTheme.colorScheme.error,
                  filePath = configFilePath,
                  placeholder = "config.json (capas, dimensiones, cabezas)",
                  testTag = "config_path_input",
                  buttonTestTag = "browse_config_file_button",
                  onBrowseClick = { configPickerLauncher.launch(arrayOf("*/*")) }
                )

                // 4. tokenizer_config.json (Obligatorio)
                FileSelectorField(
                  title = "4. tokenizer_config.json (Plantilla Chat):",
                  badgeText = "Obligatorio",
                  badgeColor = MaterialTheme.colorScheme.error,
                  filePath = tokenizerConfigFilePath,
                  placeholder = "tokenizer_config.json (chat_template, eos)",
                  testTag = "tokenizer_config_path_input",
                  buttonTestTag = "browse_tokenizer_config_button",
                  onBrowseClick = { tokenizerConfigPickerLauncher.launch(arrayOf("*/*")) }
                )

                // 5. generation_config.json (Opcional)
                FileSelectorField(
                  title = "5. generation_config.json (Hiperparámetros):",
                  badgeText = "Opcional",
                  badgeColor = MaterialTheme.colorScheme.outline,
                  filePath = generationConfigFilePath,
                  placeholder = "generation_config.json (temperatura default)",
                  testTag = "generation_config_path_input",
                  buttonTestTag = "browse_generation_config_button",
                  onBrowseClick = { generationConfigPickerLauncher.launch(arrayOf("*/*")) }
                )
              }
            }
          }

          // TFLite specific configs
          if (selectedFormat == ModelFormatType.TFLITE) {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
              ),
              shape = RoundedCornerShape(14.dp)
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Text(
                  text = "Archivos Complementarios para TFLite:",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )

                // 2. tokenizer.json / vocab (Opcional si es .task autocontenido)
                FileSelectorField(
                  title = "2. Tokenizador / Vocabulario (tokenizer.json / spm.model):",
                  badgeText = "Opcional / Recomendado",
                  badgeColor = MaterialTheme.colorScheme.tertiary,
                  filePath = tokenizerFilePath,
                  placeholder = "tokenizer.json, vocab.txt o tokenizer.model",
                  testTag = "tflite_tokenizer_path_input",
                  buttonTestTag = "browse_tflite_tokenizer_button",
                  onBrowseClick = { tokenizerPickerLauncher.launch(arrayOf("*/*")) }
                )

                // 3. config.json (Opcional)
                FileSelectorField(
                  title = "3. config.json (Arquitectura / Contexto):",
                  badgeText = "Opcional",
                  badgeColor = MaterialTheme.colorScheme.outline,
                  filePath = configFilePath,
                  placeholder = "config.json (capas, context_length)",
                  testTag = "tflite_config_path_input",
                  buttonTestTag = "browse_tflite_config_button",
                  onBrowseClick = { configPickerLauncher.launch(arrayOf("*/*")) }
                )
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
              val finalConfig = if (configFilePath.isNotBlank()) configFilePath else null
              val finalTokConfig = if (tokenizerConfigFilePath.isNotBlank()) tokenizerConfigFilePath else null
              val finalGenConfig = if (generationConfigFilePath.isNotBlank()) generationConfigFilePath else null

              onImport(
                finalName,
                selectedFormat,
                parameterSize,
                quantization,
                finalPath,
                finalTokenizer,
                finalConfig,
                finalTokConfig,
                finalGenConfig,
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

@Composable
private fun FileSelectorField(
  title: String,
  badgeText: String,
  badgeColor: androidx.compose.ui.graphics.Color,
  filePath: String,
  placeholder: String,
  testTag: String,
  buttonTestTag: String,
  onBrowseClick: () -> Unit
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(badgeColor.copy(alpha = 0.15f))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = badgeText,
          style = MaterialTheme.typography.labelSmall,
          color = badgeColor,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
      }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedTextField(
        value = if (filePath.isNotBlank()) filePath.substringAfterLast("/") else "",
        onValueChange = {},
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        modifier = Modifier
          .weight(1f)
          .testTag(testTag),
        readOnly = true,
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )
      Button(
        onClick = onBrowseClick,
        modifier = Modifier.testTag(buttonTestTag),
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

private fun String.capitalizeWords(): String {
  return split(" ").joinToString(" ") { word ->
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
  }
}

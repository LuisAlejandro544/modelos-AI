package com.example.ui.safetensors

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeTensorsImportScreen(
  onStartChat: (
    modelName: String,
    weightsUri: String,
    tokenizerUri: String,
    configUri: String,
    tokenizerConfigUri: String?,
    generationConfigUri: String?,
    paramSize: String,
    quantization: String,
    customPrompt: String
  ) -> Unit,
  onOpenTokenizerGuide: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var modelName by remember { mutableStateOf("") }
  var paramSize by remember { mutableStateOf("0.5B") }
  var quantization by remember { mutableStateOf("F16") }
  var customPrompt by remember { mutableStateOf("Eres un asistente de IA local y privado ejecutado con tensores SafeTensors en Android.") }
  var extractedMetadataInfo by remember { mutableStateOf<String?>(null) }

  // File URIs / Paths
  var weightsUri by remember { mutableStateOf<String?>(null) }
  var tokenizerUri by remember { mutableStateOf<String?>(null) }
  var configUri by remember { mutableStateOf<String?>(null) }
  var tokenizerConfigUri by remember { mutableStateOf<String?>(null) }
  var generationConfigUri by remember { mutableStateOf<String?>(null) }

  // Activity Result Launchers for each separate file
  val weightsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      weightsUri = it.toString()
      if (modelName.isBlank()) {
        val nameFromUri = it.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "SafeTensors Model"
        modelName = nameFromUri.replace("-", " ").replace("_", " ")
      }
    }
  }

  val tokenizerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { tokenizerUri = it.toString() }
  }

  val configLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      configUri = it.toString()
      // Auto-extract metadata from config.json
      val meta = parseConfigJson(context, it)
      if (meta != null) {
        if (meta.modelType.isNotBlank() && modelName.isBlank()) {
          modelName = meta.modelType.replaceFirstChar { char -> char.uppercase() } + " SafeTensors"
        }
        if (meta.estimatedParams.isNotBlank()) {
          paramSize = meta.estimatedParams
        }
        if (meta.torchDtype.isNotBlank()) {
          quantization = meta.torchDtype.uppercase().replace("FLOAT", "F").replace("BFLOAT", "BF")
        }
        extractedMetadataInfo = "Metadatos extraídos de config.json: Tipo=${meta.modelType}, Params=${meta.estimatedParams}, Dtype=${meta.torchDtype}"
      }
    }
  }

  val tokenizerConfigLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      tokenizerConfigUri = it.toString()
      // Auto-extract chat template & system info from tokenizer_config.json
      val tokMeta = parseTokenizerConfigJson(context, it)
      if (tokMeta != null && tokMeta.chatTemplateSummary.isNotBlank()) {
        extractedMetadataInfo = (extractedMetadataInfo?.plus("\n") ?: "") +
          "Plantilla detectada: ${tokMeta.chatTemplateSummary}"
      }
    }
  }

  val generationConfigLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { generationConfigUri = it.toString() }
  }

  // All 4 files are mandatory for accurate tokenization + architecture + weights
  val areRequiredFilesSelected = !weightsUri.isNullOrBlank() &&
    !tokenizerUri.isNullOrBlank() &&
    !configUri.isNullOrBlank() &&
    !tokenizerConfigUri.isNullOrBlank()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("safetensors_import_screen"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Configuración SafeTensors",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Carga modular de tensores, tokenizador y plantillas",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("safetensors_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Volver",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          IconButton(
            onClick = onOpenTokenizerGuide,
            modifier = Modifier.testTag("safetensors_guide_button")
          ) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = "Guía de archivos",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // Notice Banner
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Los 4 archivos obligatorios de SafeTensors",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "A diferencia de GGUF, SafeTensors separa tensores, diccionario y plantillas. Se requieren los 4 archivos obligatorios para tokenizar tu mensaje con el vocabulario exacto y aplicar la plantilla de chat del modelo.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 17.sp
            )
          }
        }
      }

      if (extractedMetadataInfo != null) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = extractedMetadataInfo!!,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 16.sp
            )
          }
        }
      }

      // Section 1: Required Files (4 Mandatory files)
      Text(
        text = "1. Archivos Obligatorios (4 Requeridos)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      // 1.1 Weights (.safetensors)
      FilePickerCard(
        title = "1. Pesos del Modelo (*.safetensors)",
        subtitle = "Matrices y capas neuronales (ej. model.safetensors)",
        selectedUri = weightsUri,
        isRequired = true,
        buttonTag = "select_weights_button",
        clearTag = "clear_weights_button",
        onSelectClick = { weightsLauncher.launch(arrayOf("*/*")) },
        onClearClick = { weightsUri = null }
      )

      // 1.2 Tokenizer (tokenizer.json)
      FilePickerCard(
        title = "2. Tokenizador (tokenizer.json)",
        subtitle = "Vocabulario BPE / WordPiece para convertir texto en IDs de tokens",
        selectedUri = tokenizerUri,
        isRequired = true,
        buttonTag = "select_tokenizer_button",
        clearTag = "clear_tokenizer_button",
        onSelectClick = { tokenizerLauncher.launch(arrayOf("*/*", "application/json")) },
        onClearClick = { tokenizerUri = null }
      )

      // 1.3 Config (config.json)
      FilePickerCard(
        title = "3. Configuración de Arquitectura (config.json)",
        subtitle = "Capas ocultas, cabezas de atención y dimensiones de tensores",
        selectedUri = configUri,
        isRequired = true,
        buttonTag = "select_config_button",
        clearTag = "clear_config_button",
        onSelectClick = { configLauncher.launch(arrayOf("*/*", "application/json")) },
        onClearClick = { configUri = null }
      )

      // 1.4 Tokenizer Config (tokenizer_config.json) - NOW MANDATORY
      FilePickerCard(
        title = "4. Tokenizer Config (tokenizer_config.json)",
        subtitle = "Plantilla de formato de chat (ChatML, Llama-3, Gemma, tokens especiales)",
        selectedUri = tokenizerConfigUri,
        isRequired = true,
        buttonTag = "select_tok_config_button",
        clearTag = "clear_tok_config_button",
        onSelectClick = { tokenizerConfigLauncher.launch(arrayOf("*/*", "application/json")) },
        onClearClick = { tokenizerConfigUri = null }
      )

      // Section 2: Optional Files
      Text(
        text = "2. Archivo Auxiliar (Opcional)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      // 2.1 Generation Config
      FilePickerCard(
        title = "5. Generation Config (generation_config.json)",
        subtitle = "Valores de fábrica de muestreo y parada (opcional)",
        selectedUri = generationConfigUri,
        isRequired = false,
        buttonTag = "select_gen_config_button",
        clearTag = "clear_gen_config_button",
        onSelectClick = { generationConfigLauncher.launch(arrayOf("*/*", "application/json")) },
        onClearClick = { generationConfigUri = null }
      )

      // Section 3: Model Details
      Text(
        text = "3. Metadatos del Modelo (Autocompletados o Personalizados)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      OutlinedTextField(
        value = modelName,
        onValueChange = { modelName = it },
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
          onValueChange = { paramSize = it },
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
          onValueChange = { quantization = it },
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
        onValueChange = { customPrompt = it },
        label = { Text("Prompt de Sistema Inicial") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("safetensors_prompt_input"),
        shape = RoundedCornerShape(12.dp),
        minLines = 2,
        maxLines = 4
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Final Action: Start Conversation
      Button(
        onClick = {
          if (areRequiredFilesSelected) {
            onStartChat(
              modelName,
              weightsUri!!,
              tokenizerUri!!,
              configUri!!,
              tokenizerConfigUri,
              generationConfigUri,
              paramSize,
              quantization,
              customPrompt
            )
          }
        },
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

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

private data class ConfigMetadata(
  val modelType: String,
  val estimatedParams: String,
  val torchDtype: String,
  val contextLength: Int
)

private data class TokenizerConfigMetadata(
  val chatTemplateSummary: String,
  val bosToken: String?,
  val eosToken: String?
)

private fun parseConfigJson(context: Context, uri: Uri): ConfigMetadata? {
  return try {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
      val reader = BufferedReader(InputStreamReader(inputStream))
      val content = reader.readText()
      val json = JSONObject(content)

      val modelType = json.optString("model_type", "")
      val hiddenSize = json.optInt("hidden_size", 0)
      val numLayers = json.optInt("num_hidden_layers", 0)
      val intermediateSize = json.optInt("intermediate_size", 0)
      val torchDtype = json.optString("torch_dtype", json.optString("dtype", "f16"))
      val maxPositions = json.optInt("max_position_embeddings", 4096)

      val estimatedParams = when {
        hiddenSize > 0 && numLayers > 0 -> {
          val totalParams = (hiddenSize.toLong() * hiddenSize * numLayers * 4) +
            (hiddenSize.toLong() * intermediateSize * numLayers * 3)
          when {
            totalParams > 2_500_000_000L -> "3B"
            totalParams > 1_200_000_000L -> "1.5B"
            totalParams > 600_000_000L -> "0.8B"
            totalParams > 250_000_000L -> "0.5B"
            else -> "0.3B"
          }
        }
        else -> ""
      }

      ConfigMetadata(
        modelType = modelType,
        estimatedParams = estimatedParams,
        torchDtype = torchDtype,
        contextLength = maxPositions
      )
    }
  } catch (e: Exception) {
    null
  }
}

private fun parseTokenizerConfigJson(context: Context, uri: Uri): TokenizerConfigMetadata? {
  return try {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
      val reader = BufferedReader(InputStreamReader(inputStream))
      val content = reader.readText()
      val json = JSONObject(content)

      val chatTemplate = json.optString("chat_template", "")
      val summary = when {
        chatTemplate.contains("<|im_start|>") -> "ChatML (<|im_start|>user / assistant)"
        chatTemplate.contains("<|start_header_id|>") -> "Llama 3 (<|start_header_id|>)"
        chatTemplate.contains("<start_of_turn>") -> "Gemma (<start_of_turn>user / model)"
        chatTemplate.contains("[INST]") -> "Mistral / Llama 2 ([INST] ... [/INST])"
        chatTemplate.isNotBlank() -> "Jinja Template Personalizado"
        else -> "Estandar"
      }

      TokenizerConfigMetadata(
        chatTemplateSummary = summary,
        bosToken = json.optString("bos_token", null),
        eosToken = json.optString("eos_token", null)
      )
    }
  } catch (e: Exception) {
    null
  }
}

@Composable
private fun FilePickerCard(
  title: String,
  subtitle: String,
  selectedUri: String?,
  isRequired: Boolean,
  buttonTag: String,
  clearTag: String,
  onSelectClick: () -> Unit,
  onClearClick: () -> Unit
) {
  val isSelected = !selectedUri.isNullOrBlank()
  val displayFileName = selectedUri?.substringAfterLast("/")?.substringAfterLast("%2F") ?: ""

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = when {
          isSelected -> MaterialTheme.colorScheme.primary
          isRequired -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
          else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        },
        shape = RoundedCornerShape(14.dp)
      ),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
      }
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Description,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else if (isRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
              if (isRequired) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
              else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = if (isRequired) "OBLIGATORIO" else "OPCIONAL",
            style = MaterialTheme.typography.labelSmall,
            color = if (isRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.5.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (isSelected) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = displayFileName.ifBlank { selectedUri!! },
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1
            )
          }

          IconButton(
            onClick = onClearClick,
            modifier = Modifier
              .size(24.dp)
              .testTag(clearTag)
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = "Quitar archivo",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      } else {
        OutlinedButton(
          onClick = onSelectClick,
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .testTag(buttonTag),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Elegir archivo desde el teléfono",
            style = MaterialTheme.typography.labelMedium
          )
        }
      }
    }
  }
}

package com.example.ui.safetensors

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.safetensors.components.FilePickerCard
import com.example.ui.safetensors.components.SafeTensorsHeaderCard
import com.example.ui.safetensors.components.SafeTensorsMetadataForm
import com.example.ui.safetensors.parser.ModelConfigParser

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
      val meta = ModelConfigParser.parseConfigJson(context, it)
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
      val tokMeta = ModelConfigParser.parseTokenizerConfigJson(context, it)
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

      SafeTensorsHeaderCard(extractedMetadataInfo = extractedMetadataInfo)

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

      // 1.4 Tokenizer Config (tokenizer_config.json)
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

      // Section 3: Model Details & Start Button
      SafeTensorsMetadataForm(
        modelName = modelName,
        onModelNameChange = { modelName = it },
        paramSize = paramSize,
        onParamSizeChange = { paramSize = it },
        quantization = quantization,
        onQuantizationChange = { quantization = it },
        customPrompt = customPrompt,
        onCustomPromptChange = { customPrompt = it },
        areRequiredFilesSelected = areRequiredFilesSelected,
        onStartInference = {
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
      )

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

package com.example.ui.welcome

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LocalAiModel
import com.example.viewmodel.SystemSpecs

@Composable
fun WelcomeScreen(
  selectedModel: LocalAiModel?,
  systemSpecs: SystemSpecs,
  onSelectGgufFile: (uri: String, displayName: String?) -> Unit,
  onOpenSafeTensorsFlow: () -> Unit,
  onStartChatClick: () -> Unit,
  onChangeModelClick: () -> Unit,
  onOpenParametersClick: () -> Unit,
  onOpenTokenizerGuide: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // GGUF single file picker launcher with filename extraction
  val ggufLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      val displayName = extractDisplayNameFromUri(context, it)
      onSelectGgufFile(it.toString(), displayName)
    }
  }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("welcome_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Hero Image with smooth rounded frame
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .clip(RoundedCornerShape(22.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_local_ai_welcome_1787179230263),
          contentDescription = "Ilustración de IA local offline",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0x9915191E)),
                startY = 90f
              )
            )
        )

        // Privacy Tag on image
        Row(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "100% Offline & Privado",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Title & Subtitle
      Text(
        text = "Inteligencia Artificial Local",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Elige el modo según el formato de tu modelo descargado para iniciar la inferencia en tu teléfono.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Hardware Specs Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Memory,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Hardware Móvil",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            IconButton(
              onClick = onOpenParametersClick,
              modifier = Modifier.size(30.dp).testTag("welcome_quick_settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Ajustar parámetros",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            SystemStatItem(
              label = "Procesador",
              value = "${systemSpecs.availableCores} núcleos CPU"
            )
            SystemStatItem(
              label = "Acelerador",
              value = if (systemSpecs.hasNpu) "NPU Activa" else "GPU (Vulkan)"
            )
            SystemStatItem(
              label = "Mapeo Flash",
              value = "mmap ON"
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Section: Mode Selector Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Selecciona un Modo de Carga:",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(
          onClick = onOpenTokenizerGuide,
          modifier = Modifier.size(28.dp).testTag("welcome_guide_icon_button")
        ) {
          Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = "Guía de formatos",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // MODE 1: GGUF Direct Mode (1 single file)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
          .clickable { ggufLauncher.launch(arrayOf("*/*")) }
          .testTag("mode_gguf_card"),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Modo GGUF (.gguf)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "1 solo archivo autocontenido (llama.cpp)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "MÁS RÁPIDO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Solo selecciona tu archivo .gguf (ej: smollm-360m.gguf, qwen2.5-0.5b.gguf) y comenzará la conversación de inmediato.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { ggufLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("select_gguf_file_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Seleccionar archivo .GGUF", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // MODE 2: SafeTensors Multi-File Mode
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
          .clickable(onClick = onOpenSafeTensorsFlow)
          .testTag("mode_safetensors_card"),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Layers,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Modo SafeTensors (.safetensors)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Carga modular en Rust (Candle)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "MODULAR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Abre una pantalla dedicada para elegir el archivo de tensores (.safetensors) junto con tokenizer.json y config.json por separado antes de iniciar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedButton(
            onClick = onOpenSafeTensorsFlow,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("open_safetensors_config_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Layers,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Configurar archivos SafeTensors", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
          }
        }
      }

      // If a model is currently loaded in memory, show resume chat card
      if (selectedModel != null) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "MODELO CARGADO",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                  text = selectedModel.name,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              OutlinedButton(
                onClick = onChangeModelClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("change_model_button")
              ) {
                Text("Gestionar", fontSize = 11.sp)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = onStartChatClick,
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("resume_chat_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
              Text("Reanudar Conversación", fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun SystemStatItem(
  label: String,
  value: String
) {
  Column(horizontalAlignment = Alignment.Start) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.sp
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

private fun extractDisplayNameFromUri(context: Context, uri: Uri): String? {
  var name: String? = null
  if (uri.scheme == "content") {
    try {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (index >= 0) {
            name = cursor.getString(index)
          }
        }
      }
    } catch (_: Exception) {}
  }
  if (name.isNullOrBlank()) {
    name = uri.lastPathSegment?.substringAfterLast("/")
  }
  return name?.removeSuffix(".gguf")
    ?.replace("-", " ")
    ?.replace("_", " ")
    ?.trim()
}

package com.example.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
  selectedModel: LocalAiModel?,
  parameters: InferenceParameters,
  isClearEnabled: Boolean,
  onBackClick: () -> Unit,
  onOpenModelSelector: () -> Unit,
  onOpenParameters: () -> Unit,
  onClearChatRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    windowInsets = WindowInsets(0, 0, 0, 0),
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface
    ),
    navigationIcon = {
      IconButton(
        onClick = onBackClick,
        modifier = Modifier.testTag("chat_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Volver a bienvenida",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    title = {
      // Clickable Model Chip
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
          .clickable(onClick = onOpenModelSelector)
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("active_model_chip"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Memory,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = selectedModel?.name ?: "Sin modelo cargado",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = "Cambiar modelo",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
          Text(
            text = "${selectedModel?.parameterSize ?: "Auto"} • ${parameters.accelerator.badge} • ${if (parameters.useMmap) "mmap ON" else "RAM"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
          )
        }
      }
    },
    actions = {
      // Parameters Button
      IconButton(
        onClick = onOpenParameters,
        modifier = Modifier.testTag("chat_parameters_button")
      ) {
        Icon(
          imageVector = Icons.Default.Tune,
          contentDescription = "Parámetros de inferencia",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }

      // Clear Chat Button
      IconButton(
        onClick = onClearChatRequest,
        enabled = isClearEnabled,
        modifier = Modifier.testTag("chat_clear_button")
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Limpiar conversación",
          tint = if (isClearEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
      }
    }
  )
}

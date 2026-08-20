package com.example.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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

@Composable
fun ChatInputBar(
  inputText: String,
  onInputChange: (String) -> Unit,
  onSendClick: () -> Unit,
  onStopClick: () -> Unit,
  isGenerating: Boolean,
  parameters: InferenceParameters,
  selectedModel: LocalAiModel?,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
      .navigationBarsPadding()
      .imePadding()
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    // Quick Parameter info row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Temp: ${parameters.temperature} • Acelerador: ${parameters.accelerator.badge} • ${if (parameters.useMmap) "mmap ON" else "RAM direct"} • Max: ${parameters.maxTokens} tok",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Offline",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.tertiary,
          fontWeight = FontWeight.Bold,
          fontSize = 10.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Input row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputChange,
        placeholder = {
          Text(
            text = "Pregunta a ${selectedModel?.name?.split(" ")?.firstOrNull() ?: "la IA"}...",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("chat_input_field"),
        maxLines = 4,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
      )

      Spacer(modifier = Modifier.width(8.dp))

      if (isGenerating) {
        // Stop button
        IconButton(
          onClick = onStopClick,
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .testTag("stop_generation_button")
        ) {
          Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Detener generación",
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(22.dp)
          )
        }
      } else {
        // Send button
        IconButton(
          onClick = onSendClick,
          enabled = inputText.isNotBlank(),
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(
              if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            .testTag("send_message_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Enviar mensaje",
            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

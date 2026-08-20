package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.ChatMessage
import com.example.model.ChatRole
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  selectedModel: LocalAiModel?,
  parameters: InferenceParameters,
  messages: List<ChatMessage>,
  isGenerating: Boolean,
  liveTokensPerSec: Double?,
  liveHardwareInfo: String,
  approximateTokens: Int,
  contextLimit: Int,
  contextPercentage: Float,
  showClearDialog: Boolean,
  onSendMessage: (String) -> Unit,
  onStopGeneration: () -> Unit,
  onClearChatRequest: () -> Unit,
  onClearChatConfirm: () -> Unit,
  onClearChatDismiss: () -> Unit,
  onOpenModelSelector: () -> Unit,
  onOpenParameters: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val context = LocalContext.current

  // Scroll to bottom when new messages arrive or while streaming
  LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("chat_screen"),
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
      ) {
        TopAppBar(
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
              enabled = messages.isNotEmpty() || isGenerating,
              modifier = Modifier.testTag("chat_clear_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Limpiar conversación",
                tint = if (messages.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
              )
            }
          }
        )

        // Context Window Limit & Hardware Accelerator Meter Bar
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
          )
        ) {
          Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Storage,
                  contentDescription = null,
                  tint = if (contextPercentage > 85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = "Contexto: ~$approximateTokens / $contextLimit tokens (${String.format(java.util.Locale.US, "%.1f", contextPercentage)}%)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 10.5.sp
                )
              }

              // Hardware Badge with live t/s during generation
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (isGenerating && liveTokensPerSec != null) {
                  Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "$liveTokensPerSec t/s",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                  Text(
                    text = if (parameters.accelerator.name == "AUTO") "GPU Auto" else parameters.accelerator.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Context window progress bar
            LinearProgressIndicator(
              progress = { (contextPercentage / 100f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
              color = when {
                contextPercentage > 90f -> MaterialTheme.colorScheme.error
                contextPercentage > 75f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
              },
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }
      }
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
            onValueChange = { inputText = it },
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
              onClick = onStopGeneration,
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
              onClick = {
                if (inputText.isNotBlank()) {
                  val text = inputText
                  inputText = ""
                  onSendMessage(text)
                }
              },
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
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (messages.isEmpty()) {
        // Empty State with Starter Prompts
        EmptyChatWelcome(
          model = selectedModel,
          onPromptClick = { prompt ->
            inputText = prompt
          }
        )
      } else {
        // Messages LazyColumn
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(messages, key = { it.id }) { message ->
            MessageBubble(
              message = message,
              onCopy = { text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Mensaje IA", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
              }
            )
          }
        }
      }
    }
  }

  // Clear Chat Confirmation Dialog
  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = onClearChatDismiss,
      title = {
        Text("¿Limpiar conversación?", fontWeight = FontWeight.Bold)
      },
      text = {
        Text("Se borrarán todos los mensajes del chat actual. La configuración de parámetros se mantendrá intacta.")
      },
      confirmButton = {
        TextButton(
          onClick = onClearChatConfirm,
          modifier = Modifier.testTag("confirm_clear_chat_button")
        ) {
          Text("Limpiar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = onClearChatDismiss,
          modifier = Modifier.testTag("cancel_clear_chat_button")
        ) {
          Text("Cancelar")
        }
      }
    )
  }
}

@Composable
private fun EmptyChatWelcome(
  model: LocalAiModel?,
  onPromptClick: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = if (model != null) "Listo para chatear con ${model.name}" else "Inicia una conversación",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Aceleración GPU/NPU/CPU • mmap optimizado • 100% privado",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Sugerencias para comenzar:",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(10.dp))

    val suggestions = listOf(
      "¿Cómo funciona el mapeo mmap en modelos locales?",
      "¿Qué diferencia hay entre inferencia por GPU, NPU y CPU?",
      "¿Cuántos tokens puede procesar este modelo?",
      "Escribe un script en Kotlin para Android"
    )

    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      suggestions.forEach { prompt ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable { onPromptClick(prompt) }
            .testTag("suggestion_card"),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = prompt,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 13.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MessageBubble(
  message: ChatMessage,
  onCopy: (String) -> Unit
) {
  val isUser = message.role == ChatRole.USER

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
  ) {
    if (!isUser) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(11.dp)
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = message.metrics?.modelName ?: "IA Local",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.sp
        )
      }
    }

    // Message Card
    Surface(
      modifier = Modifier
        .widthIn(max = 330.dp)
        .clip(
          RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
          )
        )
        .border(
          width = 1.dp,
          color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
          )
        ),
      color = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
      } else {
        MaterialTheme.colorScheme.surface
      },
      tonalElevation = if (isUser) 0.dp else 2.dp
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        if (message.content.isEmpty() && message.isStreaming) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Generando respuesta local...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
          }
        } else {
          Text(
            text = message.content + if (message.isStreaming) " ▍" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp,
            fontSize = 14.sp
          )
        }

        // Metrics and Actions for Assistant Messages
        if (!isUser) {
          if (message.isStreaming && message.liveTokensPerSec != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(11.dp)
              )
              Text(
                text = "${message.liveTokensPerSec} tok/s • ${message.liveHardwareInfo ?: "GPU"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              )
            }
          } else if (!message.isStreaming && message.content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              message.metrics?.let { metrics ->
                Column {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Speed,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(11.dp)
                    )
                    Text(
                      text = "${metrics.hardwareUsed} • ${metrics.tokensPerSecond} t/s",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontSize = 10.5.sp
                    )
                  }
                  Text(
                    text = "${metrics.tokensGenerated} tokens en ${metrics.generationTimeMs}ms • ${if (metrics.isMmapEnabled) "mmap ON" else "RAM"} • ${metrics.ramUsageMb} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp
                  )
                }
              } ?: Spacer(modifier = Modifier.width(1.dp))

              IconButton(
                onClick = { onCopy(message.content) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copiar respuesta",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

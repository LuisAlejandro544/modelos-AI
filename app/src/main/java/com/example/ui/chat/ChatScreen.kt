package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.InferenceParameters
import com.example.model.LocalAiModel
import com.example.ui.chat.components.ChatInputBar
import com.example.ui.chat.components.ChatMessageBubble
import com.example.ui.chat.components.ChatTopBar
import com.example.ui.chat.components.ChatWelcomeSuggestions
import com.example.ui.chat.components.ContextMeterBar

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
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .statusBarsPadding()
      ) {
        ChatTopBar(
          selectedModel = selectedModel,
          parameters = parameters,
          isClearEnabled = messages.isNotEmpty() || isGenerating,
          onBackClick = onBackClick,
          onOpenModelSelector = onOpenModelSelector,
          onOpenParameters = onOpenParameters,
          onClearChatRequest = onClearChatRequest
        )

        ContextMeterBar(
          parameters = parameters,
          approximateTokens = approximateTokens,
          contextLimit = contextLimit,
          contextPercentage = contextPercentage,
          isGenerating = isGenerating,
          liveTokensPerSec = liveTokensPerSec
        )
      }
    },
    bottomBar = {
      ChatInputBar(
        inputText = inputText,
        onInputChange = { inputText = it },
        onSendClick = {
          if (inputText.isNotBlank()) {
            val text = inputText
            inputText = ""
            onSendMessage(text)
          }
        },
        onStopClick = onStopGeneration,
        isGenerating = isGenerating,
        parameters = parameters,
        selectedModel = selectedModel
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(MaterialTheme.colorScheme.background)
    ) {
      if (messages.isEmpty()) {
        ChatWelcomeSuggestions(
          model = selectedModel,
          onPromptClick = { prompt ->
            inputText = prompt
          }
        )
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(messages, key = { it.id }) { message ->
            ChatMessageBubble(
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

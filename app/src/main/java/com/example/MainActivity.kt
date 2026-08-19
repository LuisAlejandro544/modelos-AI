package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.chat.ChatScreen
import com.example.ui.dialogs.ImportModelDialog
import com.example.ui.dialogs.ModelSelectorDialog
import com.example.ui.dialogs.ParametersDialog
import com.example.ui.dialogs.TokenizerGuideDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.welcome.WelcomeScreen
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.CurrentScreen

class MainActivity : ComponentActivity() {

  private val viewModel: ChatViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        LocalAiApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun LocalAiApp(viewModel: ChatViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    AnimatedContent(
      targetState = state.currentScreen,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "screen_transition",
      modifier = Modifier.padding(innerPadding)
    ) { screen ->
      when (screen) {
        CurrentScreen.WELCOME -> {
          WelcomeScreen(
            selectedModel = state.selectedModel,
            systemSpecs = state.systemSpecs,
            onStartChatClick = { viewModel.navigateTo(CurrentScreen.CHAT) },
            onChangeModelClick = { viewModel.showModelSelector(true) },
            onOpenParametersClick = { viewModel.showParameters(true) }
          )
        }

        CurrentScreen.CHAT -> {
          ChatScreen(
            selectedModel = state.selectedModel,
            parameters = state.parameters,
            messages = state.messages,
            isGenerating = state.isGenerating,
            showClearDialog = state.showClearChatDialog,
            onSendMessage = { viewModel.sendMessage(it) },
            onStopGeneration = { viewModel.stopGeneration() },
            onClearChatRequest = { viewModel.showClearChatConfirm(true) },
            onClearChatConfirm = { viewModel.clearChat() },
            onClearChatDismiss = { viewModel.showClearChatConfirm(false) },
            onOpenModelSelector = { viewModel.showModelSelector(true) },
            onOpenParameters = { viewModel.showParameters(true) },
            onBackClick = { viewModel.navigateTo(CurrentScreen.WELCOME) }
          )
        }
      }
    }
  }

  // Model Selector Dialog
  if (state.showModelSelectorDialog) {
    ModelSelectorDialog(
      selectedModel = state.selectedModel,
      allModels = state.allAvailableModels,
      onModelSelected = { viewModel.selectModel(it) },
      onOpenImportDialog = { viewModel.showImportDialog(true) },
      onOpenTokenizerGuide = { viewModel.showTokenizerGuide(true) },
      onDeleteCustomModel = { viewModel.deleteCustomModel(it) },
      onDismiss = { viewModel.showModelSelector(false) }
    )
  }

  // Import Model Dialog
  if (state.showImportDialog) {
    ImportModelDialog(
      onImport = { name, format, paramSize, quant, path, tokenizer, prompt ->
        viewModel.importCustomModel(name, format, paramSize, quant, path, tokenizer, prompt)
      },
      onOpenTokenizerGuide = { viewModel.showTokenizerGuide(true) },
      onDismiss = { viewModel.showImportDialog(false) }
    )
  }

  // Tokenizer Guide Dialog
  if (state.showTokenizerGuideDialog) {
    TokenizerGuideDialog(
      onDismiss = { viewModel.showTokenizerGuide(false) }
    )
  }

  // Parameters Dialog
  if (state.showParametersDialog) {
    ParametersDialog(
      currentParameters = state.parameters,
      maxAvailableCores = state.systemSpecs.availableCores,
      onSave = { viewModel.updateParameters(it) },
      onReset = { viewModel.resetParameters() },
      onDismiss = { viewModel.showParameters(false) }
    )
  }
}

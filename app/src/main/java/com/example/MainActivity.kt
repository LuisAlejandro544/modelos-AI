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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.chat.ChatScreen
import com.example.ui.dialogs.ChatHistoryDialog
import com.example.ui.dialogs.ImportModelDialog
import com.example.ui.dialogs.ModelSelectorDialog
import com.example.ui.dialogs.ParametersDialog
import com.example.ui.dialogs.TokenizerGuideDialog
import com.example.ui.safetensors.SafeTensorsImportScreen
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

  AnimatedContent(
    targetState = state.currentScreen,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "screen_transition",
    modifier = Modifier.fillMaxSize()
  ) { screen ->
    when (screen) {
      CurrentScreen.WELCOME -> {
        WelcomeScreen(
          selectedModel = state.selectedModel,
          savedModels = state.customModels,
          chatSessions = state.chatSessions,
          systemSpecs = state.systemSpecs,
          onSelectGgufFile = { uri, name -> viewModel.loadGgufModelDirect(uri, name) },
          onOpenSafeTensorsFlow = { viewModel.openSafeTensorsImport() },
          onEditSafeTensors = { viewModel.openEditSafeTensors(it) },
          onSelectSavedModel = {
            viewModel.selectModel(it)
            viewModel.startNewChat(it)
          },
          onDeleteSavedModel = { viewModel.deleteCustomModel(it) },
          onOpenChatHistory = { viewModel.showHistoryDialog(true) },
          onSelectChatSession = { viewModel.openChatSession(it) },
          onStartChatClick = {
            if (state.currentSessionId == null) {
              viewModel.startNewChat(state.selectedModel)
            } else {
              viewModel.navigateTo(CurrentScreen.CHAT)
            }
          },
          onChangeModelClick = { viewModel.showModelSelector(true) },
          onOpenParametersClick = { viewModel.showParameters(true) },
          onOpenTokenizerGuide = { viewModel.showTokenizerGuide(true) }
        )
      }

      CurrentScreen.IMPORT_SAFETENSORS -> {
        SafeTensorsImportScreen(
          initialModel = state.editingSafeTensorsModel,
          onStartChat = { name, weights, tokenizer, config, tokConfig, genConfig, paramSize, quant, prompt ->
            viewModel.importOrUpdateSafeTensorsBundle(
              modelName = name,
              weightsUri = weights,
              tokenizerUri = tokenizer,
              configUri = config,
              tokenizerConfigUri = tokConfig,
              generationConfigUri = genConfig,
              paramSize = paramSize,
              quantization = quant,
              customPrompt = prompt
            )
          },
          onOpenTokenizerGuide = { viewModel.showTokenizerGuide(true) },
          onBackClick = { viewModel.navigateTo(CurrentScreen.WELCOME) }
        )
      }

      CurrentScreen.CHAT -> {
        ChatScreen(
          selectedModel = state.selectedModel,
          parameters = state.parameters,
          messages = state.messages,
          isGenerating = state.isGenerating,
          liveTokensPerSec = state.liveTokensPerSec,
          liveHardwareInfo = state.liveHardwareInfo,
          approximateTokens = state.approximateConversationTokens,
          contextLimit = state.contextLimit,
          contextPercentage = state.contextUsagePercentage,
          showClearDialog = state.showClearChatDialog,
          onSendMessage = { viewModel.sendMessage(it) },
          onStopGeneration = { viewModel.stopGeneration() },
          onClearChatRequest = { viewModel.showClearChatConfirm(true) },
          onClearChatConfirm = { viewModel.clearChat() },
          onClearChatDismiss = { viewModel.showClearChatConfirm(false) },
          onOpenModelSelector = { viewModel.showModelSelector(true) },
          onOpenParameters = { viewModel.showParameters(true) },
          onOpenHistory = { viewModel.showHistoryDialog(true) },
          onBackClick = { viewModel.navigateTo(CurrentScreen.WELCOME) }
        )
      }
    }
  }

  // Chat History Dialog
  if (state.showHistoryDialog) {
    ChatHistoryDialog(
      sessions = state.chatSessions,
      currentSessionId = state.currentSessionId,
      onSelectSession = { viewModel.openChatSession(it) },
      onNewChat = { viewModel.startNewChat() },
      onDeleteSession = { viewModel.deleteChatSession(it) },
      onRenameSession = { id, newTitle -> viewModel.renameChatSession(id, newTitle) },
      onDismiss = { viewModel.showHistoryDialog(false) }
    )
  }

  // Model Selector Dialog
  if (state.showModelSelectorDialog) {
    ModelSelectorDialog(
      selectedModel = state.selectedModel,
      allModels = state.allAvailableModels,
      onModelSelected = { viewModel.selectModel(it) },
      onOpenImportDialog = { viewModel.showImportDialog(true) },
      onOpenTokenizerGuide = { viewModel.showTokenizerGuide(true) },
      onEditSafeTensors = {
        viewModel.showModelSelector(false)
        viewModel.openEditSafeTensors(it)
      },
      onDeleteCustomModel = { viewModel.deleteCustomModel(it) },
      onDismiss = { viewModel.showModelSelector(false) }
    )
  }

  // Import Model Dialog
  if (state.showImportDialog) {
    ImportModelDialog(
      onImport = { name, format, paramSize, quant, path, tokenizer, config, tokConfig, genConfig, prompt ->
        viewModel.importCustomModel(name, format, paramSize, quant, path, tokenizer, config, tokConfig, genConfig, prompt)
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
      selectedModel = state.selectedModel,
      maxAvailableCores = state.systemSpecs.availableCores,
      onSave = { viewModel.updateParameters(it) },
      onReset = { viewModel.resetParameters() },
      onDismiss = { viewModel.showParameters(false) }
    )
  }
}

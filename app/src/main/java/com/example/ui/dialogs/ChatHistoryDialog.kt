package com.example.ui.dialogs

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.ChatSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatHistoryDialog(
  sessions: List<ChatSessionEntity>,
  currentSessionId: String?,
  onSelectSession: (ChatSessionEntity) -> Unit,
  onNewChat: () -> Unit,
  onDeleteSession: (String) -> Unit,
  onRenameSession: (String, String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var sessionToRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
  var sessionToDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = modifier
        .fillMaxWidth()
        .testTag("chat_history_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Forum,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Historial de Chats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${sessions.size} conversaciones locales",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_history_dialog_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cerrar historial",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // New Chat Button
        Button(
          onClick = {
            onNewChat()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("start_new_chat_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Iniciar Nueva Conversación",
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Session List
        if (sessions.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Sin conversaciones guardadas",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Los mensajes se guardan automáticamente aquí",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(sessions, key = { it.id }) { session ->
              val isSelected = session.id == currentSessionId
              SessionItemCard(
                session = session,
                isSelected = isSelected,
                onSelect = { onSelectSession(session) },
                onRename = { sessionToRename = session },
                onDelete = { sessionToDelete = session }
              )
            }
          }
        }
      }
    }
  }

  // Rename Dialog
  sessionToRename?.let { session ->
    var newTitle by remember(session) { mutableStateOf(session.title) }
    AlertDialog(
      onDismissRequest = { sessionToRename = null },
      title = { Text("Renombrar conversación", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Ingresa el nuevo título para esta conversación:")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("rename_session_input")
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (newTitle.isNotBlank()) {
              onRenameSession(session.id, newTitle)
              sessionToRename = null
            }
          },
          modifier = Modifier.testTag("confirm_rename_button")
        ) {
          Text("Guardar", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { sessionToRename = null }) {
          Text("Cancelar")
        }
      }
    )
  }

  // Delete Dialog
  sessionToDelete?.let { session ->
    AlertDialog(
      onDismissRequest = { sessionToDelete = null },
      title = { Text("¿Eliminar conversación?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Se borrará permanentemente '${session.title}' y todos sus mensajes asociados.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteSession(session.id)
            sessionToDelete = null
          },
          modifier = Modifier.testTag("confirm_delete_session_button")
        ) {
          Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { sessionToDelete = null }) {
          Text("Cancelar")
        }
      }
    )
  }
}

@Composable
private fun SessionItemCard(
  session: ChatSessionEntity,
  isSelected: Boolean,
  onSelect: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
  val formattedDate = remember(session.updatedAt) { dateFormat.format(Date(session.updatedAt)) }

  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  val backgroundColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(backgroundColor)
      .border(1.dp, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onSelect)
      .padding(12.dp)
      .testTag("session_item_${session.id}")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Activo",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = session.title.ifBlank { "Conversación" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onRename,
            modifier = Modifier
              .size(32.dp)
              .testTag("rename_session_${session.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Renombrar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(32.dp)
              .testTag("delete_session_${session.id}")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Eliminar",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Snippet
      if (session.lastSnippet.isNotBlank()) {
        Text(
          text = session.lastSnippet,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
      }

      // Metadata Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = session.modelName.ifBlank { "Modelo local" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Text(
          text = "$formattedDate • ${session.messageCount} msgs",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          fontSize = 10.sp
        )
      }
    }
  }
}

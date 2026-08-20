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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun TokenizerGuideDialog(
  onDismiss: () -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .clip(RoundedCornerShape(24.dp))
        .testTag("tokenizer_guide_dialog"),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth()
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Guía: Archivos y Modelos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "¿Cuáles necesitas descargar y cuáles no?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_tokenizer_guide_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cerrar",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Concept card
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Regla de Oro: GGUF vs SafeTensors",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "• En GGUF (.gguf): Solo descargas 1 archivo. Contiene pesos, tokenizador y configs en su interior.\n• En SafeTensors (.safetensors): Se compone de varios archivos separados en Hugging Face.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 18.sp
                )
              }
            }
          }

          // Breakdown of Files in Hugging Face
          Text(
            text = "Desglose de Archivos en SafeTensors:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          FileExplanationCard(
            fileName = "1. model.safetensors",
            badgeText = "OBLIGATORIO",
            badgeColor = MaterialTheme.colorScheme.error,
            description = "Contiene los millones de pesos y matrices numéricas de la red neuronal.",
            isRequired = true
          )

          FileExplanationCard(
            fileName = "2. tokenizer.json",
            badgeText = "OBLIGATORIO",
            badgeColor = MaterialTheme.colorScheme.error,
            description = "El diccionario y reglas para traducir tus palabras a números (IDs de tokens) y viceversa.",
            isRequired = true
          )

          FileExplanationCard(
            fileName = "3. config.json",
            badgeText = "OBLIGATORIO",
            badgeColor = MaterialTheme.colorScheme.error,
            description = "El plano del modelo: define la cantidad de capas, cabezas de atención y longitud de contexto.",
            isRequired = true
          )

          FileExplanationCard(
            fileName = "4. tokenizer_config.json",
            badgeText = "RECOMENDADO",
            badgeColor = MaterialTheme.colorScheme.tertiary,
            description = "Contiene la plantilla de chat (chat_template) y tokens de fin de frase (<|im_end|>).",
            isRequired = false
          )

          FileExplanationCard(
            fileName = "5. generation_config.json",
            badgeText = "OPCIONAL",
            badgeColor = MaterialTheme.colorScheme.outline,
            description = "Configuración de fábrica de temperatura y top_p (la app te permite ajustarlos en tiempo real).",
            isRequired = false
          )

          FileExplanationCard(
            fileName = "6. training_args.bin",
            badgeText = "IGNORAR / NO DESCARGAR",
            badgeColor = Color.Gray,
            description = "Solo guarda registros del entrenamiento original. Es inútil para inferencia en el teléfono.",
            isRequired = false
          )

          // How to get them
          Text(
            text = "Pasos para descargar en Hugging Face:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              GuideStepItem(
                step = "1",
                title = "Entra a huggingface.co",
                detail = "Busca tu modelo deseado (ej: HuggingFaceTB/SmolLM-360M-Instruct o Qwen/Qwen2.5-0.5B)."
              )
              GuideStepItem(
                step = "2",
                title = "Pestaña 'Files and versions'",
                detail = "Descarga model.safetensors, tokenizer.json, config.json y tokenizer_config.json."
              )
              GuideStepItem(
                step = "3",
                title = "Carga en AI Local",
                detail = "Toca 'Importar Modelo Propio', elige formato SafeTensors y asigna los archivos descargados."
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("understand_tokenizer_guide_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("Entendido", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun FileExplanationCard(
  fileName: String,
  badgeText: String,
  badgeColor: Color,
  description: String,
  isRequired: Boolean
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isRequired) 1.dp else 0.5.dp,
        color = badgeColor.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
      ),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = fileName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeColor.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
          )
        }
      }
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.5.sp,
        lineHeight = 16.sp
      )
    }
  }
}

@Composable
private fun GuideStepItem(
  step: String,
  title: String,
  detail: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = step,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }
  }
}

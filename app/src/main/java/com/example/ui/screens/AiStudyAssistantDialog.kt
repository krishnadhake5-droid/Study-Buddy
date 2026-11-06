package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.AiAssistantState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiStudyAssistantBottomSheet(
  aiState: AiAssistantState,
  defaultSubject: String,
  onAskAi: (String, String, String) -> Unit,
  onSaveResponseAsNote: (String, String, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedMode by remember { mutableStateOf("UNDERSTAND") }
  var subject by remember { mutableStateOf(defaultSubject.ifBlank { "Artificial Intelligence" }) }
  var promptText by remember {
    mutableStateOf("Explain A* Search Algorithm and admissible heuristics with a simple real-life example.")
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "AI Study Assistant",
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "Powered by Gemini • Adapts to your degree & subject",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close AI Assistant")
        }
      }

      // 3 Learning Modes
      Text(
        text = "Select Learning Mode:",
        style = MaterialTheme.typography.labelLarge,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(
          "UNDERSTAND" to "1. Understand",
          "PRACTICE" to "2. Practice Quiz",
          "REVISION" to "3. Flashcards & Notes",
        ).forEach { (code, label) ->
          FilterChip(
            selected = selectedMode == code,
            onClick = { selectedMode = code },
            label = { Text(label) },
          )
        }
      }

      OutlinedTextField(
        value = subject,
        onValueChange = { subject = it },
        label = { Text("Academic Subject") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = promptText,
        onValueChange = { promptText = it },
        label = {
          Text(
            when (selectedMode) {
              "UNDERSTAND" -> "Concept to explain simply with examples"
              "PRACTICE" -> "Topic to generate practice questions & solutions"
              else -> "Topic or notes to summarize into flashcards & revision points"
            }
          )
        },
        minLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
      )

      // Quick Prompt Suggestions
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SuggestionChip(
          onClick = {
            subject = "Artificial Intelligence"
            promptText = "Explain Minimax & Alpha-Beta Pruning step by step."
          },
          label = { Text("Minimax & Alpha-Beta") },
        )
        SuggestionChip(
          onClick = {
            subject = "Mathematics"
            promptText = "Create 4 practice problems on Eigenvalues & Eigenvectors with solutions."
          },
          label = { Text("Eigenvalues Practice") },
        )
        SuggestionChip(
          onClick = {
            subject = "Operating Systems"
            promptText = "Generate flashcards and key revision bullets for Deadlock Prevention & Banker's Algorithm."
          },
          label = { Text("Deadlocks Flashcards") },
        )
      }

      Button(
        onClick = { onAskAi(selectedMode, subject, promptText) },
        enabled = aiState !is AiAssistantState.Loading,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("ask_ai_submit_button"),
        shape = RoundedCornerShape(14.dp),
      ) {
        if (aiState is AiAssistantState.Loading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Thinking...")
        } else {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Generate with AI Study Assistant", fontWeight = FontWeight.Bold)
        }
      }

      val responseText =
        when (aiState) {
          is AiAssistantState.Streaming -> aiState.text
          is AiAssistantState.Success -> aiState.text
          else -> null
        }

      if (responseText != null) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Study Buddy AI ($selectedMode Mode)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              FilledTonalButton(
                onClick = {
                  onSaveResponseAsNote(
                    "AI $selectedMode: ${promptText.take(40)}",
                    subject,
                    responseText,
                    if (selectedMode == "REVISION") "FLASHCARD" else "NOTE",
                  )
                },
                modifier = Modifier.height(34.dp),
              ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save to Notes", style = MaterialTheme.typography.labelMedium)
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = responseText,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      if (aiState is AiAssistantState.Error) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.errorContainer
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = aiState.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

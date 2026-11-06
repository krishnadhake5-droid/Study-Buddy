package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SyllabusTopic

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SyllabusPlannerScreen(
  topics: List<SyllabusTopic>,
  onAddTopic: (String, String, String, String, String) -> Unit,
  onCycleTopicStatus: (SyllabusTopic) -> Unit,
  onDeleteTopic: (String) -> Unit,
  onExtractSyllabusWithAi: (String, String) -> Unit,
) {
  val subjects =
    remember(topics) {
      listOf("All Subjects") + topics.map { it.subject }.distinct().sorted()
    }
  var selectedSubject by remember { mutableStateOf("All Subjects") }
  var showAddTopicDialog by remember { mutableStateOf(false) }
  var showAiExtractDialog by remember { mutableStateOf(false) }

  val filteredTopics =
    remember(topics, selectedSubject) {
      if (selectedSubject == "All Subjects") topics
      else topics.filter { it.subject == selectedSubject }
    }

  val subjectProgressList =
    remember(topics) {
      topics.groupBy { it.subject }.map { (sub, list) ->
        val completed = list.count { it.status == "COMPLETED" }
        val pct = if (list.isNotEmpty()) (completed * 100) / list.size else 0
        Triple(sub, "$completed/${list.size} topics", pct)
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Header & AI Syllabus Planner Action
    item {
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Smart Syllabus Planner",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Track subjects, units, chapters, and topics. Paste syllabus text or upload an outline to let Gemini AI automatically extract and distribute topics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = { showAiExtractDialog = true },
              modifier = Modifier.weight(1f).testTag("ai_syllabus_extract_button"),
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("AI Syllabus Import")
            }
            FilledTonalButton(
              onClick = { showAddTopicDialog = true },
              modifier = Modifier.weight(1f).testTag("manual_add_topic_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Topic")
            }
          }
        }
      }
    }

    // 2. Subject-wise Syllabus Completion Summary Cards
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Subject Completion Breakdown",
            style = MaterialTheme.typography.titleMedium,
          )
          subjectProgressList.forEach { (sub, countLabel, pct) ->
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = sub,
                  style = MaterialTheme.typography.labelLarge,
                )
                Text(
                  text = "$pct% ($countLabel)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
              )
            }
          }
        }
      }
    }

    // 3. Subject Filter Chips
    item {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        subjects.forEach { subject ->
          FilterChip(
            selected = selectedSubject == subject,
            onClick = { selectedSubject = subject },
            label = { Text(subject) },
          )
        }
      }
    }

    // 4. Syllabus Topics List
    items(filteredTopics, key = { it.id }) { topic ->
      SyllabusTopicCard(
        topic = topic,
        onCycleStatus = { onCycleTopicStatus(topic) },
        onDelete = { onDeleteTopic(topic.id) },
      )
    }
  }

  if (showAddTopicDialog) {
    var subject by remember {
      mutableStateOf(
        if (selectedSubject != "All Subjects") selectedSubject else "Artificial Intelligence"
      )
    }
    var unitName by remember { mutableStateOf("Unit 3 – Knowledge Representation") }
    var chapterName by remember { mutableStateOf("First-Order Logic") }
    var topicTitle by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("MEDIUM") }

    AlertDialog(
      onDismissRequest = { showAddTopicDialog = false },
      title = { Text("Add Syllabus Topic") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = unitName,
            onValueChange = { unitName = it },
            label = { Text("Unit (e.g., Unit 1 – Introduction)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = chapterName,
            onValueChange = { chapterName = it },
            label = { Text("Chapter Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = topicTitle,
            onValueChange = { topicTitle = it },
            label = { Text("Individual Topic Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("EASY", "MEDIUM", "HARD").forEach { diff ->
              FilterChip(
                selected = difficulty == diff,
                onClick = { difficulty = diff },
                label = { Text(diff) },
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (topicTitle.isNotBlank()) {
              onAddTopic(subject, unitName, chapterName, topicTitle, difficulty)
              showAddTopicDialog = false
            }
          }
        ) {
          Text("Add Topic")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showAiExtractDialog) {
    var subject by remember { mutableStateOf("Database Systems") }
    var rawSyllabus by remember {
      mutableStateOf(
        "Unit 1: Relational Model & SQL - ER Diagrams, Relational Algebra, Complex SQL Joins.\nUnit 2: Normalization & Transactions - 1NF/2NF/3NF/BCNF, ACID Properties, Two-Phase Locking."
      )
    }

    AlertDialog(
      onDismissRequest = { showAiExtractDialog = false },
      title = { Text("AI Syllabus Topic Extractor") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Paste syllabus text from your PDF or course handout. Gemini will extract units, chapters, and topics with difficulty ratings:",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = rawSyllabus,
            onValueChange = { rawSyllabus = it },
            label = { Text("Syllabus Outline / Document Text") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rawSyllabus.isNotBlank()) {
              onExtractSyllabusWithAi(rawSyllabus, subject.ifBlank { "General" })
              showAiExtractDialog = false
            }
          }
        ) {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Extract & Schedule")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAiExtractDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun SyllabusTopicCard(
  topic: SyllabusTopic,
  onCycleStatus: () -> Unit,
  onDelete: () -> Unit,
) {
  val statusColor =
    when (topic.status) {
      "COMPLETED" -> Color(0xFF059669)
      "STUDYING" -> Color(0xFF2563EB)
      "NEEDS_REVISION" -> Color(0xFFD97706)
      else -> Color(0xFF64748B)
    }

  val statusLabel =
    when (topic.status) {
      "COMPLETED" -> "Completed"
      "STUDYING" -> "Currently Studying"
      "NEEDS_REVISION" -> "Needs Revision"
      else -> "Not Started"
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${topic.subject} • ${topic.unitName}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = topic.difficulty,
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Delete topic",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Text(
        text = topic.topicTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "Chapter: ${topic.chapterName} • Revisions: ${topic.revisionCount}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = statusColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelLarge,
            color = statusColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
        FilledTonalButton(
          onClick = onCycleStatus,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(36.dp),
        ) {
          Icon(
            imageVector =
              if (topic.status == "COMPLETED") Icons.Default.Replay
              else Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Update Status", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.AcademicDeadline
import com.example.data.AcademicTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerAndDeadlinesScreen(
  tasks: List<AcademicTask>,
  deadlines: List<AcademicDeadline>,
  onAddTask: (String, String, String, String, String, Long, String) -> Unit,
  onToggleTaskCompleted: (AcademicTask) -> Unit,
  onRescheduleTask: (AcademicTask, String) -> Unit,
  onDeleteTask: (String) -> Unit,
  onAddDeadline: (String, String, String, String, Long, String) -> Unit,
  onToggleDeadlineCompleted: (AcademicDeadline) -> Unit,
  onDeleteDeadline: (String) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var taskToReschedule by remember { mutableStateOf<AcademicTask?>(null) }
  var showAddRoutineDialog by remember { mutableStateOf(false) }
  var showAddDeadlineDialog by remember { mutableStateOf(false) }

  val routineItems = remember(tasks) { tasks.filter { it.category == "ROUTINE" } }
  val todoItems = remember(tasks) { tasks.filter { it.category == "TODO" } }

  Column(modifier = Modifier.fillMaxSize()) {
    PrimaryTabRow(selectedTabIndex = selectedTab) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("Daily Schedule (${routineItems.size})") },
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Deadlines & Exams (${deadlines.size})") },
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("To-Do List (${todoItems.size})") },
      )
    }

    when (selectedTab) {
      0 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Smart Dynamic Daily Scheduler",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = "Missed a study block? Tap 'Reschedule' to move it to an open evening slot without overcrowding.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = { showAddRoutineDialog = true }) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Slot")
                }
              }
            }
          }

          items(routineItems, key = { it.id }) { item ->
            RoutineTimelineCard(
              item = item,
              onToggleCompleted = { onToggleTaskCompleted(item) },
              onRescheduleClick = { taskToReschedule = item },
              onDelete = { onDeleteTask(item.id) },
            )
          }
        }
      }

      1 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Assignment & Exam Manager",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = "Track assignments, practicals, projects, and exams",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(
                onClick = { showAddDeadlineDialog = true },
                modifier = Modifier.testTag("add_deadline_button"),
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
              }
            }
          }

          items(deadlines.sortedBy { it.daysRemaining }, key = { it.id }) { deadline ->
            DeadlineCard(
              deadline = deadline,
              onToggleCompleted = { onToggleDeadlineCompleted(deadline) },
              onDelete = { onDeleteDeadline(deadline.id) },
            )
          }
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Academic & Personal To-Do List",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = "Quick tasks, library returns, and study errands",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(onClick = { showAddRoutineDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add To-Do")
              }
            }
          }

          if (todoItems.isEmpty()) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  ),
              ) {
                Text(
                  text = "No extra to-dos pending! Tap 'Add To-Do' to add personal or academic errands.",
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(20.dp),
                )
              }
            }
          } else {
            items(todoItems, key = { it.id }) { item ->
              RoutineTimelineCard(
                item = item,
                onToggleCompleted = { onToggleTaskCompleted(item) },
                onRescheduleClick = { taskToReschedule = item },
                onDelete = { onDeleteTask(item.id) },
              )
            }
          }
        }
      }
    }
  }

  if (taskToReschedule != null) {
    val task = taskToReschedule!!
    var newSlot by remember { mutableStateOf("09:15 PM - 10:00 PM (Evening Recovery Slot)") }
    AlertDialog(
      onDismissRequest = { taskToReschedule = null },
      title = { Text("Smart Dynamic Rescheduler") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Missed '${task.title}'? Pick a realistic open slot without overcrowding your evening rest:",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = newSlot,
            onValueChange = { newSlot = it },
            label = { Text("New Available Time Slot") },
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = newSlot.startsWith("07:15 PM"),
              onClick = { newSlot = "07:15 PM - 08:00 PM" },
              label = { Text("7:15 PM") },
            )
            FilterChip(
              selected = newSlot.startsWith("09:00 PM"),
              onClick = { newSlot = "09:00 PM - 09:45 PM" },
              label = { Text("9:00 PM") },
            )
            FilterChip(
              selected = newSlot.startsWith("Tomorrow"),
              onClick = { newSlot = "Tomorrow 06:30 AM - 07:15 AM" },
              label = { Text("Tomorrow AM") },
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onRescheduleTask(task, newSlot)
            taskToReschedule = null
          }
        ) {
          Text("Reschedule Slot")
        }
      },
      dismissButton = {
        TextButton(onClick = { taskToReschedule = null }) { Text("Cancel") }
      },
    )
  }

  if (showAddRoutineDialog) {
    val defaultCat = if (selectedTab == 2) "TODO" else "ROUTINE"
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Study Session") }
    var note by remember { mutableStateOf("") }
    var slot by remember { mutableStateOf("06:00 PM - 06:45 PM") }
    var duration by remember { mutableStateOf("45") }

    AlertDialog(
      onDismissRequest = { showAddRoutineDialog = false },
      title = { Text(if (defaultCat == "TODO") "Add To-Do Item" else "Add Schedule Activity") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Activity / Task Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject / Category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = slot,
            onValueChange = { slot = it },
            label = { Text("Time Slot (e.g., 05:00 PM - 05:45 PM)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Details / Notes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank()) {
              onAddTask(
                title,
                subject.ifBlank { "General" },
                note,
                defaultCat,
                slot,
                duration.toLongOrNull() ?: 45L,
                "MEDIUM",
              )
              showAddRoutineDialog = false
            }
          }
        ) {
          Text("Add")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddRoutineDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showAddDeadlineDialog) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Artificial Intelligence") }
    var type by remember { mutableStateOf("ASSIGNMENT") }
    var dueDate by remember { mutableStateOf("2026-11-20") }
    var daysStr by remember { mutableStateOf("14") }
    var priority by remember { mutableStateOf("HIGH") }

    AlertDialog(
      onDismissRequest = { showAddDeadlineDialog = false },
      title = { Text("Add Academic Deadline / Exam") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title (e.g., Internal Exam 2, Lab Report)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ASSIGNMENT", "EXAM", "PROJECT", "PRACTICAL").forEach { t ->
              FilterChip(
                selected = type == t,
                onClick = { type = t },
                label = { Text(t) },
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = dueDate,
              onValueChange = { dueDate = it },
              label = { Text("Due Date") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = daysStr,
              onValueChange = { daysStr = it.filter { ch -> ch.isDigit() } },
              label = { Text("Days Left") },
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank()) {
              onAddDeadline(
                title,
                subject.ifBlank { "General" },
                type,
                dueDate.ifBlank { "2026-11-25" },
                daysStr.toLongOrNull() ?: 10L,
                priority,
              )
              showAddDeadlineDialog = false
            }
          }
        ) {
          Text("Save Deadline")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDeadlineDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun RoutineTimelineCard(
  item: AcademicTask,
  onToggleCompleted: () -> Unit,
  onRescheduleClick: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (item.completed)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(8.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = item.timeSlot,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
        Row {
          IconButton(onClick = onToggleCompleted, modifier = Modifier.size(34.dp)) {
            Icon(
              imageVector =
                if (item.completed) Icons.Default.CheckCircle
                else Icons.Default.RadioButtonUnchecked,
              contentDescription = "Toggle completion",
              tint =
                if (item.completed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Delete activity",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
      )
      if (item.chapterOrTopic.isNotBlank()) {
        Text(
          text = "${item.subject} • ${item.chapterOrTopic}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (!item.completed) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
          onClick = onRescheduleClick,
          modifier = Modifier.height(36.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        ) {
          Icon(
            imageVector = Icons.Default.EventRepeat,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Missed? Reschedule Slot", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

@Composable
private fun DeadlineCard(
  deadline: AcademicDeadline,
  onToggleCompleted: () -> Unit,
  onDelete: () -> Unit,
) {
  val badgeColor =
    when {
      deadline.daysRemaining <= 5 -> Color(0xFFDC2626)
      deadline.daysRemaining <= 14 -> Color(0xFFD97706)
      else -> Color(0xFF2563EB)
    }

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onToggleCompleted) {
        Icon(
          imageVector =
            if (deadline.completed) Icons.Default.CheckCircle
            else Icons.Default.Assignment,
          contentDescription = "Toggle deadline completion",
          tint = if (deadline.completed) MaterialTheme.colorScheme.primary else badgeColor,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = badgeColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = "${deadline.type} • ${deadline.daysRemaining}d left",
              style = MaterialTheme.typography.labelMedium,
              color = badgeColor,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "Due ${deadline.dueDateLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = deadline.title,
          style = MaterialTheme.typography.titleMedium,
          textDecoration =
            if (deadline.completed) TextDecoration.LineThrough else TextDecoration.None,
        )
        Text(
          text = deadline.subject,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete deadline",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

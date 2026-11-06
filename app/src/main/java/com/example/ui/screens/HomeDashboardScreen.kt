package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AcademicDeadline
import com.example.data.AcademicTask
import com.example.data.StudentProfile
import com.example.data.SyllabusTopic
import java.util.Calendar

@Composable
fun HomeDashboardScreen(
  profile: StudentProfile?,
  tasks: List<AcademicTask>,
  topics: List<SyllabusTopic>,
  deadlines: List<AcademicDeadline>,
  aiPersonalizationTip: String,
  onStartTaskFocus: (AcademicTask) -> Unit,
  onToggleTaskCompleted: (AcademicTask) -> Unit,
  onDeleteTask: (String) -> Unit,
  onAddTask: (String, String, String, String, String, Long, String) -> Unit,
  onRefreshAiInsight: () -> Unit,
  onOpenAiAssistant: () -> Unit,
) {
  var showAddGoalDialog by remember { mutableStateOf(false) }

  val greeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when {
      hour < 12 -> "Good Morning"
      hour < 17 -> "Good Afternoon"
      else -> "Good Evening"
    }
  }
  val studentName = profile?.studentName?.ifBlank { "Scholar" } ?: "Scholar"

  val dailyGoals = remember(tasks) { tasks.filter { it.category == "GOAL" } }
  val totalGoals = dailyGoals.size
  val completedGoals = dailyGoals.count { it.completed }
  val remainingGoals = (totalGoals - completedGoals).coerceAtLeast(0)
  val completionRatio = if (totalGoals > 0) completedGoals.toFloat() / totalGoals else 0f
  val completionPercent = (completionRatio * 100).toInt()

  val studiedMinutesToday =
    dailyGoals.filter { it.completed }.sumOf { it.durationMinutes }
  val remainingMinutesToday =
    dailyGoals.filter { !it.completed }.sumOf { it.durationMinutes }

  val completedTopicsCount = topics.count { it.status == "COMPLETED" }
  val totalTopicsCount = topics.size.coerceAtLeast(1)
  val syllabusCompletionPercent = (completedTopicsCount * 100) / totalTopicsCount

  val nextExam =
    deadlines.filter { it.type == "EXAM" && !it.completed }.minByOrNull { it.daysRemaining }
      ?: deadlines.firstOrNull { !it.completed }

  val motivationalQuotes = remember {
    listOf(
      "\"Small progress every day leads to big achievements.\"",
      "\"Consistency beats intensity. Focus on one chapter at a time.\"",
      "\"Rest is part of the work—your brain consolidates memory during breaks.\"",
    )
  }
  val dailyQuote = remember { motivationalQuotes[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % motivationalQuotes.size] }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Greeting Hero Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Box(modifier = Modifier.fillMaxWidth().height(168.dp)) {
          Image(
            painter = painterResource(id = R.drawable.img_study_hero_1793981566149),
            contentDescription = "Study dashboard banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors =
                      listOf(
                        Color(0xFF0F172A).copy(alpha = 0.88f),
                        Color(0xFF1E3A8A).copy(alpha = 0.65f),
                      )
                  )
                )
          )
          Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "$greeting, $studentName!",
                  style = MaterialTheme.typography.headlineSmall,
                  color = Color.White,
                )
                Text(
                  text = "Let's make today productive.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.88f),
                )
              }
              Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                shape = RoundedCornerShape(50),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Study streak",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${profile?.streakDays ?: 5}d Streak",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                  )
                }
              }
            }

            Surface(
              color = Color.White.copy(alpha = 0.14f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = dailyQuote,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    }

    // 2. Daily Progress Tracker Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Daily Progress Tracker",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Daily academic goal: $totalGoals tasks • Completed: $completedGoals • Remaining: $remainingGoals",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(68.dp)) {
              CircularProgressIndicator(
                progress = { completionRatio },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 7.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
              )
              Text(
                text = "$completionPercent%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            MetricMiniPill(
              modifier = Modifier.weight(1f),
              label = "Studied Today",
              value = "${studiedMinutesToday}m",
            )
            MetricMiniPill(
              modifier = Modifier.weight(1f),
              label = "Remaining Time",
              value = "${remainingMinutesToday}m",
            )
            MetricMiniPill(
              modifier = Modifier.weight(1f),
              label = "Topics Done",
              value = "$completedTopicsCount/${topics.size}",
            )
            MetricMiniPill(
              modifier = Modifier.weight(1f),
              label = "Focus Sessions",
              value = "${profile?.focusSessionsCompleted ?: 3}",
            )
          }
        }
      }
    }

    // 3. Examination Countdown & Smart AI Personalization Engine
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Exam Countdown Card
        Card(
          modifier = Modifier.weight(1f),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Exam Countdown",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "${nextExam?.daysRemaining ?: 25} Days Left",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = nextExam?.title ?: "Semester Examination",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
              maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { syllabusCompletionPercent / 100f },
              modifier = Modifier.fillMaxWidth().height(6.dp),
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Syllabus: $syllabusCompletionPercent% ($completedTopicsCount/${topics.size} topics)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }

    // 4. AI Personalization Engine Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
          ),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Smart AI Personalization Engine",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Row {
              IconButton(
                onClick = onRefreshAiInsight,
                modifier = Modifier.size(36.dp).testTag("refresh_ai_insight_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh AI recommendation",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = aiPersonalizationTip,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          Spacer(modifier = Modifier.height(10.dp))
          FilledTonalButton(
            onClick = onOpenAiAssistant,
            modifier = Modifier.fillMaxWidth().testTag("open_ai_study_assistant_button"),
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask AI Study Assistant (Understand • Practice • Revise)")
          }
        }
      }
    }

    // 5. Today's Academic Goals Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Today's Academic Goals",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Tap Start to enter Smart Focus Mode or mark tasks completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(
          onClick = { showAddGoalDialog = true },
          modifier = Modifier.testTag("add_daily_goal_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Goal")
        }
      }
    }

    if (dailyGoals.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "No daily academic goals added yet.",
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tap 'Add Goal' to set your targets for today.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(dailyGoals, key = { it.id }) { goal ->
        AcademicGoalCard(
          goal = goal,
          onStartFocus = { onStartTaskFocus(goal) },
          onToggleCompleted = { onToggleTaskCompleted(goal) },
          onDelete = { onDeleteTask(goal.id) },
        )
      }
    }
  }

  if (showAddGoalDialog) {
    AddAcademicGoalDialog(
      onDismiss = { showAddGoalDialog = false },
      onConfirm = { title, subject, topic, timeSlot, duration, priority ->
        onAddTask(title, subject, topic, "GOAL", timeSlot, duration, priority)
        showAddGoalDialog = false
      },
    )
  }
}

@Composable
private fun MetricMiniPill(
  modifier: Modifier = Modifier,
  label: String,
  value: String,
) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun AcademicGoalCard(
  goal: AcademicTask,
  onStartFocus: () -> Unit,
  onToggleCompleted: () -> Unit,
  onDelete: () -> Unit,
) {
  val priorityColor =
    when (goal.priority) {
      "HIGH" -> Color(0xFFDC2626)
      "MEDIUM" -> Color(0xFFD97706)
      else -> Color(0xFF059669)
    }

  Card(
    modifier = Modifier.fillMaxWidth().testTag("goal_card_${goal.id}"),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (goal.completed)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surface
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = if (goal.completed) 0.dp else 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = goal.subject,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Surface(
            color = priorityColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = "${goal.priority} PRIORITY",
              style = MaterialTheme.typography.labelMedium,
              color = priorityColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${goal.durationMinutes} min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Delete goal",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = goal.title,
        style = MaterialTheme.typography.titleMedium,
        textDecoration = if (goal.completed) TextDecoration.LineThrough else TextDecoration.None,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (goal.chapterOrTopic.isNotBlank()) {
        Text(
          text = "Topic: ${goal.chapterOrTopic} • ${goal.timeSlot}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = onToggleCompleted,
          modifier = Modifier.weight(1f).testTag("toggle_goal_${goal.id}"),
        ) {
          Icon(
            imageVector =
              if (goal.completed) Icons.Default.CheckCircle
              else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (goal.completed) "Completed" else "Mark Complete")
        }
        if (!goal.completed) {
          Button(
            onClick = onStartFocus,
            modifier = Modifier.weight(1f).testTag("start_goal_focus_${goal.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Start Focus")
          }
        }
      }
    }
  }
}

@Composable
private fun AddAcademicGoalDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, Long, String) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf("Artificial Intelligence") }
  var topic by remember { mutableStateOf("") }
  var timeSlot by remember { mutableStateOf("06:00 PM - 06:45 PM") }
  var durationStr by remember { mutableStateOf("45") }
  var priority by remember { mutableStateOf("HIGH") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Today's Academic Goal") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Goal Title (e.g., Solve 15 practice problems)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = topic,
          onValueChange = { topic = it },
          label = { Text("Chapter or Topic") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = timeSlot,
            onValueChange = { timeSlot = it },
            label = { Text("Time Slot") },
            singleLine = true,
            modifier = Modifier.weight(1.3f),
          )
          OutlinedTextField(
            value = durationStr,
            onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
            label = { Text("Mins") },
            singleLine = true,
            modifier = Modifier.weight(0.7f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
            FilterChip(
              selected = priority == p,
              onClick = { priority = p },
              label = { Text(p) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onConfirm(
              title,
              subject.ifBlank { "General" },
              topic,
              timeSlot,
              durationStr.toLongOrNull() ?: 45L,
              priority,
            )
          }
        }
      ) {
        Text("Save Goal")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

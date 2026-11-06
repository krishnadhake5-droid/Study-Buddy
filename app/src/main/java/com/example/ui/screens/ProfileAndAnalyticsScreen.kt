package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AcademicTask
import com.example.data.AppUsageLimit
import com.example.data.PeerStudyGroup
import com.example.data.StudentProfile
import com.example.data.StudyNote
import com.example.data.SyllabusTopic
import com.google.firebase.auth.auth

@Composable
fun ProfileAndAnalyticsScreen(
  profile: StudentProfile?,
  verifiedMobileNumber: String? = null,
  tasks: List<AcademicTask>,
  topics: List<SyllabusTopic>,
  notes: List<StudyNote>,
  appLimits: List<AppUsageLimit>,
  studyGroups: List<PeerStudyGroup>,
  onAddNoteOrReflection: (String, String, String, String) -> Unit,
  onDeleteNote: (String) -> Unit,
  onCreateStudyGroup: (String, String, String) -> Unit,
  onJoinStudyGroup: (PeerStudyGroup) -> Unit,
  onUpdateProfile: (StudentProfile) -> Unit,
  onSignOut: () -> Unit,
) {
  var selectedSection by remember { mutableIntStateOf(0) }
  var showAddNoteDialog by remember { mutableStateOf(false) }
  var showReflectionDialog by remember { mutableStateOf(false) }
  var showAddGroupDialog by remember { mutableStateOf(false) }

  val sectionTitles =
    listOf(
      "Progress Reports",
      "Smart Notes & Flashcards",
      "Attendance & Reflection",
      "Peer Study Groups",
      "Profile & Settings",
    )

  Column(modifier = Modifier.fillMaxSize()) {
    ScrollableTabRow(selectedTabIndex = selectedSection, edgePadding = 16.dp) {
      sectionTitles.forEachIndexed { index, title ->
        Tab(
          selected = selectedSection == index,
          onClick = { selectedSection = index },
          text = { Text(title) },
        )
      }
    }

    when (selectedSection) {
      0 ->
        AnalyticsReportsSection(
          profile = profile,
          tasks = tasks,
          topics = topics,
          appLimits = appLimits,
        )

      1 ->
        SmartNotesSection(
          notes = notes.filter { it.noteType in listOf("NOTE", "FLASHCARD", "RESOURCE") },
          onAddNoteClick = { showAddNoteDialog = true },
          onDeleteNote = onDeleteNote,
        )

      2 ->
        AttendanceAndReflectionSection(
          attendanceLogs = notes.filter { it.noteType == "ATTENDANCE" },
          reflections = notes.filter { it.noteType == "REFLECTION" },
          onAddAttendance = { subject, attended, total ->
            val pct = if (total > 0) (attended * 100) / total else 100
            onAddNoteOrReflection(
              "Attendance: $subject ($attended/$total Lectures - $pct%)",
              subject,
              "Attended: $attended | Missed: ${(total - attended).coerceAtLeast(0)} | Attendance: $pct%",
              "ATTENDANCE",
            )
          },
          onOpenReflectionSurvey = { showReflectionDialog = true },
          onDeleteEntry = onDeleteNote,
        )

      3 ->
        PeerStudyGroupsSection(
          groups = studyGroups,
          onCreateGroupClick = { showAddGroupDialog = true },
          onJoinGroup = onJoinStudyGroup,
        )

      else ->
        StudentProfileSettingsSection(
          profile = profile,
          verifiedMobileNumber = verifiedMobileNumber,
          onUpdateProfile = onUpdateProfile,
          onSignOut = onSignOut,
        )
    }
  }

  if (showAddNoteDialog) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Artificial Intelligence") }
    var content by remember { mutableStateOf("") }
    var noteType by remember { mutableStateOf("NOTE") }

    AlertDialog(
      onDismissRequest = { showAddNoteDialog = false },
      title = { Text("Add Smart Note / Flashcard / Resource") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("NOTE", "FLASHCARD", "RESOURCE").forEach { type ->
              FilterChip(
                selected = noteType == type,
                onClick = { noteType = type },
                label = { Text(type) },
              )
            }
          }
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
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
          OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Notes / Q&A Flashcard / Resource Link") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (title.isNotBlank() && content.isNotBlank()) {
              onAddNoteOrReflection(title, subject, content, noteType)
              showAddNoteDialog = false
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showReflectionDialog) {
    var goalsCompletedAnswer by remember { mutableStateOf("Completed most high-priority goals") }
    var studiedSubjects by remember { mutableStateOf("Artificial Intelligence & Mathematics") }
    var difficultTopic by remember { mutableStateOf("Alpha-Beta Pruning edge cases") }
    var distractionCause by remember { mutableStateOf("Short YouTube videos after dinner") }
    var tomorrowImprovement by remember { mutableStateOf("Start Session 1 right at 5:00 PM") }

    AlertDialog(
      onDismissRequest = { showReflectionDialog = false },
      title = { Text("End-of-Day Student Reflection") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Your answers help Study Buddy tune tomorrow's schedule without pressure:",
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = goalsCompletedAnswer,
            onValueChange = { goalsCompletedAnswer = it },
            label = { Text("1. How did today's goals & timetable feel?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = studiedSubjects,
            onValueChange = { studiedSubjects = it },
            label = { Text("2. Which subjects did you study?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = difficultTopic,
            onValueChange = { difficultTopic = it },
            label = { Text("3. What did you find difficult?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = distractionCause,
            onValueChange = { distractionCause = it },
            label = { Text("4. What distracted you?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = tomorrowImprovement,
            onValueChange = { tomorrowImprovement = it },
            label = { Text("5. What would you like to improve tomorrow?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val summary =
              "• Timetable feel: $goalsCompletedAnswer\n• Studied: $studiedSubjects\n• Difficult: $difficultTopic\n• Distraction: $distractionCause\n• Tomorrow's focus: $tomorrowImprovement"
            onAddNoteOrReflection(
              "Daily Reflection (${studiedSubjects.take(30)})",
              studiedSubjects,
              summary,
              "REFLECTION",
            )
            showReflectionDialog = false
          }
        ) {
          Text("Save Reflection")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReflectionDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showAddGroupDialog) {
    var groupName by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Artificial Intelligence") }
    var activeTopic by remember { mutableStateOf("Unit 2 Revision & Past Papers") }

    AlertDialog(
      onDismissRequest = { showAddGroupDialog = false },
      title = { Text("Create Peer Study Group") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Study Group Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Academic Subject") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = activeTopic,
            onValueChange = { activeTopic = it },
            label = { Text("Current Discussion / Revision Topic") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (groupName.isNotBlank()) {
              onCreateStudyGroup(groupName, subject, activeTopic)
              showAddGroupDialog = false
            }
          }
        ) {
          Text("Create Group")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddGroupDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun AnalyticsReportsSection(
  profile: StudentProfile?,
  tasks: List<AcademicTask>,
  topics: List<SyllabusTopic>,
  appLimits: List<AppUsageLimit>,
) {
  val goals = tasks.filter { it.category == "GOAL" }
  val completedGoals = goals.count { it.completed }
  val pendingGoals = (goals.size - completedGoals).coerceAtLeast(0)
  val totalFocusMins = profile?.totalFocusMinutes ?: 180L
  val weeklyHours = (totalFocusMins / 60f) + 14.5f
  val completedTopics = topics.count { it.status == "COMPLETED" }
  val syllabusPct = if (topics.isNotEmpty()) (completedTopics * 100) / topics.size else 0
  val totalDistractionMins = appLimits.sumOf { it.usedTodayMinutes }

  val weeklyBars =
    listOf(
      "Mon" to 0.72f,
      "Tue" to 0.85f,
      "Wed" to 0.92f,
      "Thu" to 0.65f,
      "Fri" to 0.80f,
      "Sat" to 0.95f,
      "Sun" to 0.78f,
    )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Achievements & Streak Banner
    item {
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(36.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "${profile?.streakDays ?: 5}-Day Study Consistency Milestone!",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = "Supportive Note: Every focused session counts. If you miss a task, Study Buddy adjusts tomorrow's routine automatically.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    }

    // Daily, Weekly & Monthly Summary Cards
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Daily, Weekly & Monthly Academic Report", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = "• Daily Report: ${profile?.focusSessionsCompleted ?: 4} focus sessions (${totalFocusMins}m total) • $completedGoals completed / $pendingGoals pending goals • Distraction usage: ${totalDistractionMins}m",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "• Weekly Report: ${String.format(java.util.Locale.US, "%.1f", weeklyHours)} total study hours • Most productive day: Saturday • Avg daily goal completion: 82%",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "• Monthly Report: Overall syllabus completion: $syllabusPct% ($completedTopics/${topics.size} topics) • High consistency in Artificial Intelligence & Mathematics.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    }

    // Weekly Study Consistency Bar Chart
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Weekly Study Hours & Goal Completion Trend",
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
          ) {
            weeklyBars.forEach { (day, ratio) ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
              ) {
                Text(
                  text = "${(ratio * 100).toInt()}%",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                  modifier =
                    Modifier.width(26.dp)
                      .height((80 * ratio).dp)
                      .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                      .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = day, style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SmartNotesSection(
  notes: List<StudyNote>,
  onAddNoteClick: () -> Unit,
  onDeleteNote: (String) -> Unit,
) {
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
          Text("Smart Notes, Flashcards & Resources", style = MaterialTheme.typography.titleLarge)
          Text(
            "Saved subject-wise for offline & exam revision",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = onAddNoteClick) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Note")
        }
      }
    }

    items(notes, key = { it.id }) { note ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              color = MaterialTheme.colorScheme.secondaryContainer,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "${note.noteType} • ${note.subject}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note")
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = note.title, style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = note.content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun AttendanceAndReflectionSection(
  attendanceLogs: List<StudyNote>,
  reflections: List<StudyNote>,
  onAddAttendance: (String, Int, Int) -> Unit,
  onOpenReflectionSurvey: () -> Unit,
  onDeleteEntry: (String) -> Unit,
) {
  var subName by remember { mutableStateOf("Operating Systems") }
  var attendedStr by remember { mutableStateOf("28") }
  var totalStr by remember { mutableStateOf("32") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Daily Reflection Check-In", style = MaterialTheme.typography.titleMedium)
            Text(
              "Review what worked today, what distracted you, and tune tomorrow's routine.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(onClick = onOpenReflectionSurvey) {
            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reflect")
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Log Subject Lecture Attendance", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = subName,
              onValueChange = { subName = it },
              label = { Text("Subject") },
              singleLine = true,
              modifier = Modifier.weight(1.4f),
            )
            OutlinedTextField(
              value = attendedStr,
              onValueChange = { attendedStr = it.filter { ch -> ch.isDigit() } },
              label = { Text("Attended") },
              singleLine = true,
              modifier = Modifier.weight(0.8f),
            )
            OutlinedTextField(
              value = totalStr,
              onValueChange = { totalStr = it.filter { ch -> ch.isDigit() } },
              label = { Text("Total") },
              singleLine = true,
              modifier = Modifier.weight(0.8f),
            )
          }
          FilledTonalButton(
            onClick = {
              onAddAttendance(
                subName.ifBlank { "General" },
                attendedStr.toIntOrNull() ?: 1,
                totalStr.toIntOrNull() ?: 1,
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Save Attendance Record")
          }
        }
      }
    }

    items(attendanceLogs + reflections, key = { it.id }) { item ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.titleSmall)
            Text(
              text = item.content,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = { onDeleteEntry(item.id) }) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete entry")
          }
        }
      }
    }
  }
}

@Composable
private fun PeerStudyGroupsSection(
  groups: List<PeerStudyGroup>,
  onCreateGroupClick: () -> Unit,
  onJoinGroup: (PeerStudyGroup) -> Unit,
) {
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
          Text("Peer Study Groups", style = MaterialTheme.typography.titleLarge)
          Text(
            "Collaborate with classmates on difficult subjects",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(onClick = onCreateGroupClick) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Create Group")
        }
      }
    }

    items(groups, key = { it.id }) { group ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${group.subject} • ${group.memberCount} students",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = group.groupName, style = MaterialTheme.typography.titleMedium)
            Text(
              text = "Active Topic: ${group.activeTopic} (Host: ${group.creatorName})",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          FilledTonalButton(onClick = { onJoinGroup(group) }) {
            Text("Join (+1)")
          }
        }
      }
    }
  }
}

@Composable
private fun StudentProfileSettingsSection(
  profile: StudentProfile?,
  verifiedMobileNumber: String?,
  onUpdateProfile: (StudentProfile) -> Unit,
  onSignOut: () -> Unit,
) {
  val current = profile ?: StudentProfile()
  val verifiedPhone = verifiedMobileNumber ?: com.google.firebase.Firebase.auth.currentUser?.phoneNumber
  var name by remember(current) { mutableStateOf(current.studentName) }
  var degree by remember(current) { mutableStateOf(current.gradeOrDegree) }
  var college by remember(current) { mutableStateOf(current.institutionName) }
  var subjects by remember(current) { mutableStateOf(current.subjectsCsv) }
  var examDate by remember(current) { mutableStateOf(current.examStartDate) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Student Profile & Schedule Settings", style = MaterialTheme.typography.titleLarge)
              if (!verifiedPhone.isNullOrBlank()) {
                Text(
                  text = "Verified Mobile: $verifiedPhone",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Student Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = degree,
            onValueChange = { degree = it },
            label = { Text("Class / Year / Degree") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = college,
            onValueChange = { college = it },
            label = { Text("School / College") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = subjects,
            onValueChange = { subjects = it },
            label = { Text("Subjects") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = examDate,
            onValueChange = { examDate = it },
            label = { Text("Upcoming Examination Date") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Button(
            onClick = {
              onUpdateProfile(
                current.copy(
                  studentName = name,
                  gradeOrDegree = degree,
                  institutionName = college,
                  subjectsCsv = subjects,
                  examStartDate = examDate,
                )
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Save Profile Changes")
          }
        }
      }
    }

    item {
      OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out of Study Buddy", fontWeight = FontWeight.Bold)
      }
    }
  }
}

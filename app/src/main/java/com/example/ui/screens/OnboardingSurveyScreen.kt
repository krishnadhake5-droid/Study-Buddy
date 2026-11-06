package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.StudentProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingSurveyScreen(
  initialName: String,
  onCompleteOnboarding: (StudentProfile) -> Unit,
) {
  var currentStep by remember { mutableIntStateOf(0) }

  // Step 1: Student Profile
  var studentName by remember { mutableStateOf(initialName.ifBlank { "Aarav Sharma" }) }
  var gradeOrDegree by remember { mutableStateOf("B.Tech Computer Science (3rd Year)") }
  var institutionName by remember { mutableStateOf("National Institute of Technology") }
  var streamOrBranch by remember { mutableStateOf("Computer Science & AI") }
  var semester by remember { mutableStateOf("Semester 5") }
  var subjectsCsv by remember {
    mutableStateOf("Artificial Intelligence, Mathematics, Operating Systems, Database Systems")
  }
  var examStartDate by remember { mutableStateOf("2026-12-01") }
  var preferredLanguage by remember { mutableStateOf("English") }

  // Step 2: Daily Schedule Survey
  var wakeUpTime by remember { mutableStateOf("06:30 AM") }
  var sleepTime by remember { mutableStateOf("10:30 PM") }
  var collegeTiming by remember { mutableStateOf("09:00 AM - 04:00 PM") }
  var travelMinutes by remember { mutableFloatStateOf(45f) }
  var hasCoaching by remember { mutableStateOf(false) }
  var dailyStudyHours by remember { mutableFloatStateOf(4f) }
  var preferredStudySlot by remember { mutableStateOf("EVENING") }
  var difficultSubjectsCsv by remember { mutableStateOf("Artificial Intelligence, Mathematics") }

  // Step 3: Focus, Distractions & Rest Preferences
  var socialMediaMinutes by remember { mutableFloatStateOf(90f) }
  var sessionLengthMinutes by remember { mutableFloatStateOf(45f) }
  var breakLengthMinutes by remember { mutableFloatStateOf(10f) }
  var napPreference by remember { mutableStateOf(true) }

  fun buildProfile(): StudentProfile =
    StudentProfile(
      studentName = studentName.ifBlank { "Student" },
      gradeOrDegree = gradeOrDegree.ifBlank { "Undergraduate" },
      institutionName = institutionName,
      streamOrBranch = streamOrBranch,
      semester = semester,
      subjectsCsv = subjectsCsv.ifBlank { "Artificial Intelligence, Mathematics" },
      difficultSubjectsCsv = difficultSubjectsCsv,
      examStartDate = examStartDate.ifBlank { "2026-12-01" },
      preferredLanguage = preferredLanguage.ifBlank { "English" },
      wakeUpTime = wakeUpTime.ifBlank { "06:30 AM" },
      sleepTime = sleepTime.ifBlank { "10:30 PM" },
      collegeTiming = collegeTiming.ifBlank { "09:00 AM - 04:00 PM" },
      travelMinutes = travelMinutes.toLong(),
      hasCoaching = hasCoaching,
      dailyStudyHours = dailyStudyHours.toLong(),
      preferredStudySlot = preferredStudySlot,
      socialMediaMinutes = socialMediaMinutes.toLong(),
      sessionLengthMinutes = sessionLengthMinutes.toLong(),
      breakLengthMinutes = breakLengthMinutes.toLong(),
      napPreference = napPreference,
      onboardingCompleted = true,
      streakDays = 5L,
      totalFocusMinutes = 135L,
      focusSessionsCompleted = 3L,
    )

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).weight(1f)) {
        // Header & Skip option
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Personalized Study Setup",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = "Step ${currentStep + 1} of 3 • Questions are optional",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          TextButton(
            onClick = { onCompleteOnboarding(buildProfile()) },
            modifier = Modifier.testTag("skip_onboarding_button"),
          ) {
            Text("Skip & Generate")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
          progress = { (currentStep + 1) / 3f },
          modifier = Modifier.fillMaxWidth().height(8.dp),
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
          targetState = currentStep,
          modifier = Modifier.weight(1f),
          label = "onboarding_step",
        ) { step ->
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            when (step) {
              0 -> {
                StepHeaderCard(
                  icon = Icons.Default.Person,
                  title = "A. Academic Profile",
                  subtitle = "Tell Study Buddy about your courses and upcoming examinations.",
                )
                OutlinedTextField(
                  value = studentName,
                  onValueChange = { studentName = it },
                  label = { Text("Student Name") },
                  modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input"),
                  singleLine = true,
                )
                OutlinedTextField(
                  value = gradeOrDegree,
                  onValueChange = { gradeOrDegree = it },
                  label = { Text("Class, Year, or Degree") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  OutlinedTextField(
                    value = streamOrBranch,
                    onValueChange = { streamOrBranch = it },
                    label = { Text("Stream / Branch") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                  )
                  OutlinedTextField(
                    value = semester,
                    onValueChange = { semester = it },
                    label = { Text("Semester") },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true,
                  )
                }
                OutlinedTextField(
                  value = institutionName,
                  onValueChange = { institutionName = it },
                  label = { Text("School or College Name (Optional)") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                )
                OutlinedTextField(
                  value = subjectsCsv,
                  onValueChange = { subjectsCsv = it },
                  label = { Text("Your Subjects (comma-separated)") },
                  modifier = Modifier.fillMaxWidth(),
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  OutlinedTextField(
                    value = examStartDate,
                    onValueChange = { examStartDate = it },
                    label = { Text("Exam Start Date") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                  )
                  OutlinedTextField(
                    value = preferredLanguage,
                    onValueChange = { preferredLanguage = it },
                    label = { Text("Preferred Language") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                  )
                }
              }

              1 -> {
                StepHeaderCard(
                  icon = Icons.Default.Schedule,
                  title = "B. Daily Routine Survey",
                  subtitle = "We build a realistic timetable that respects your college hours and commute.",
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  OutlinedTextField(
                    value = wakeUpTime,
                    onValueChange = { wakeUpTime = it },
                    label = { Text("Wake-Up Time") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                  )
                  OutlinedTextField(
                    value = sleepTime,
                    onValueChange = { sleepTime = it },
                    label = { Text("Sleep Time") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                  )
                }
                OutlinedTextField(
                  value = collegeTiming,
                  onValueChange = { collegeTiming = it },
                  label = { Text("School / College Timings") },
                  modifier = Modifier.fillMaxWidth(),
                  singleLine = true,
                )
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                      text = "Daily Travel / Commute Time: ${travelMinutes.toInt()} mins",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                      value = travelMinutes,
                      onValueChange = { travelMinutes = it },
                      valueRange = 0f..180f,
                      steps = 11,
                    )
                    Text(
                      text = "Realistic Daily Self-Study Goal: ${dailyStudyHours.toInt()} hours/day",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                      value = dailyStudyHours,
                      onValueChange = { dailyStudyHours = it },
                      valueRange = 1f..10f,
                      steps = 8,
                    )
                  }
                }
                Text(
                  text = "When do you prefer studying most?",
                  style = MaterialTheme.typography.titleSmall,
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  listOf("MORNING", "AFTERNOON", "EVENING", "NIGHT").forEach { slot ->
                    FilterChip(
                      selected = preferredStudySlot == slot,
                      onClick = { preferredStudySlot = slot },
                      label = { Text(slot) },
                    )
                  }
                }
                OutlinedTextField(
                  value = difficultSubjectsCsv,
                  onValueChange = { difficultSubjectsCsv = it },
                  label = { Text("Subjects you find difficult or need extra focus") },
                  modifier = Modifier.fillMaxWidth(),
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "Do you attend coaching classes or tuition?",
                    style = MaterialTheme.typography.bodyMedium,
                  )
                  Switch(checked = hasCoaching, onCheckedChange = { hasCoaching = it })
                }
              }

              else -> {
                StepHeaderCard(
                  icon = Icons.Default.SelfImprovement,
                  title = "C. Focus, Breaks & Digital Discipline",
                  subtitle = "Balance deep study intervals with restorative breaks and healthy rest.",
                )
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                      text = "Preferred Study Session Length: ${sessionLengthMinutes.toInt()} mins",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                      value = sessionLengthMinutes,
                      onValueChange = { sessionLengthMinutes = it },
                      valueRange = 25f..90f,
                      steps = 12,
                    )
                    Text(
                      text = "Short Refreshment Break: ${breakLengthMinutes.toInt()} mins",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                      value = breakLengthMinutes,
                      onValueChange = { breakLengthMinutes = it },
                      valueRange = 5f..30f,
                      steps = 4,
                    )
                    Text(
                      text = "Current Social Media & Gaming Time: ${socialMediaMinutes.toInt()} mins/day",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                      value = socialMediaMinutes,
                      onValueChange = { socialMediaMinutes = it },
                      valueRange = 15f..240f,
                      steps = 14,
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Include Afternoon Power Nap / Rest?",
                      style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                      text = "Adds a 25-30 min recovery block after college before evening study.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Switch(checked = napPreference, onCheckedChange = { napPreference = it })
                }

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
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                      text =
                        "Study Buddy will generate a balanced daily routine for $studentName with ${sessionLengthMinutes.toInt()}m focus sessions, ${breakLengthMinutes.toInt()}m breaks, and prioritized goals for $difficultSubjectsCsv.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Footer Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (currentStep > 0) {
            OutlinedButton(
              onClick = { currentStep-- },
              modifier = Modifier.weight(1f).height(52.dp),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Back")
            }
          }
          Button(
            onClick = {
              if (currentStep < 2) {
                currentStep++
              } else {
                onCompleteOnboarding(buildProfile())
              }
            },
            modifier =
              Modifier.weight(1.4f)
                .height(52.dp)
                .testTag("onboarding_next_or_finish_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            if (currentStep < 2) {
              Text("Continue", fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(6.dp))
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            } else {
              Icon(Icons.Default.Check, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Generate My Routine", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StepHeaderCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
) {
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
      ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .background(
              MaterialTheme.colorScheme.secondary,
              shape = RoundedCornerShape(12.dp),
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondary,
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
        )
      }
    }
  }
}

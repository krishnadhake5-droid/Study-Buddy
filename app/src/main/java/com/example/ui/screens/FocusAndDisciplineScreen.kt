package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AppUsageLimit
import com.example.viewmodel.FocusSessionState
import com.example.viewmodel.FocusTimerMode
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusAndDisciplineScreen(
  focusState: FocusSessionState,
  appLimits: List<AppUsageLimit>,
  onSelectTimerMode: (FocusTimerMode, Int) -> Unit,
  onStartOrResumeTimer: () -> Unit,
  onPauseTimer: () -> Unit,
  onResetTimer: () -> Unit,
  onCompleteSessionEarly: () -> Unit,
  onSaveAppLimit: (String?, String, String, String, Long, Long, String, Boolean) -> Unit,
  onSyncDeviceUsage: () -> Unit,
) {
  val context = LocalContext.current
  var showAddLimitDialog by remember { mutableStateOf(false) }
  var editingLimit by remember { mutableStateOf<AppUsageLimit?>(null) }

  val minutes = focusState.remainingSeconds / 60
  val seconds = focusState.remainingSeconds % 60
  val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)
  val progress =
    if (focusState.totalSeconds > 0)
      focusState.remainingSeconds.toFloat() / focusState.totalSeconds
    else 1f

  val totalEntertainmentLimit = appLimits.sumOf { it.dailyLimitMinutes }
  val totalEntertainmentUsed = appLimits.sumOf { it.usedTodayMinutes }
  val remainingEntertainmentMinutes =
    (totalEntertainmentLimit - totalEntertainmentUsed).coerceAtLeast(0L)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Smart Focus Mode & Break Timer Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Surface(
            color =
              if (focusState.mode == FocusTimerMode.FOCUS_SESSION)
                MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector =
                  if (focusState.mode == FocusTimerMode.FOCUS_SESSION) Icons.Default.Shield
                  else Icons.Default.Coffee,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = focusState.mode.label,
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text =
              if (focusState.mode == FocusTimerMode.FOCUS_SESSION)
                "You're currently studying ${focusState.activeSubject}. Stay focused on your goal!"
              else "Balanced Rest: Step away from screens, hydrate, and let your mind recharge.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Circular Countdown
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp),
          ) {
            CircularProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxSize(),
              strokeWidth = 12.dp,
              color =
                if (focusState.mode == FocusTimerMode.FOCUS_SESSION)
                  MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = formattedTime,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = focusState.activeTaskTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
              )
            }
          }

          if (focusState.completedSummaryMessage != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = focusState.completedSummaryMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Mode Selector Chips
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FocusTimerMode.entries.forEach { mode ->
              FilterChip(
                selected = focusState.mode == mode,
                onClick = { onSelectTimerMode(mode, mode.defaultMinutes) },
                label = { Text("${mode.label.substringBefore(" ")} (${mode.defaultMinutes}m)") },
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Timer Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                if (focusState.isRunning) onPauseTimer() else onStartOrResumeTimer()
              },
              modifier = Modifier.weight(1.3f).height(48.dp).testTag("focus_timer_start_pause_button"),
            ) {
              Icon(
                imageVector =
                  if (focusState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (focusState.isRunning) "Pause" else "Start Session")
            }
            FilledTonalButton(
              onClick = onCompleteSessionEarly,
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Default.Check, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Complete")
            }
            OutlinedButton(
              onClick = onResetTimer,
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
            }
          }
        }
      }
    }

    // 2. Smart Break & Rest System Recommendations
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
          ),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.SelfImprovement,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Healthy Break & Rest Guide",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "• Short Break (10m): Stretch, drink water, look 20ft away, walk around.\n• Long Break (25m): Healthy snack, chat with family, listen to calm music.\n• Power Rest (20m): Short afternoon nap to restore alertness after college.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      }
    }

    // 3. Digital Discipline – Smart App Limiter Header & Summary
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Digital Discipline – Smart App Limiter",
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = "Remaining entertainment time today: $remainingEntertainmentMinutes mins (Used $totalEntertainmentUsed / $totalEntertainmentLimit mins)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            FilledTonalButton(
              onClick = { showAddLimitDialog = true },
              modifier = Modifier.testTag("add_app_limit_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add App")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = onSyncDeviceUsage,
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync Device Usage", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = {
                try {
                  context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                  )
                } catch (_: Exception) {}
              },
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Usage Permission", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }

    // 4. Individual App Usage Limits
    items(appLimits, key = { it.id }) { limit ->
      AppLimitCard(
        limit = limit,
        isFocusModeActive =
          focusState.isRunning && focusState.mode == FocusTimerMode.FOCUS_SESSION,
        onEditClick = { editingLimit = limit },
        onToggleRestricted = { restricted ->
          onSaveAppLimit(
            limit.id,
            limit.appName,
            limit.packageName,
            limit.category,
            limit.dailyLimitMinutes,
            limit.usedTodayMinutes,
            limit.entertainmentWindow,
            restricted,
          )
        },
      )
    }
  }

  if (showAddLimitDialog || editingLimit != null) {
    val existing = editingLimit
    var appName by remember { mutableStateOf(existing?.appName ?: "") }
    var packageName by remember { mutableStateOf(existing?.packageName ?: "com.instagram.android") }
    var category by remember { mutableStateOf(existing?.category ?: "Social Media") }
    var limitMinStr by remember { mutableStateOf((existing?.dailyLimitMinutes ?: 30L).toString()) }
    var usedMinStr by remember { mutableStateOf((existing?.usedTodayMinutes ?: 15L).toString()) }
    var window by remember {
      mutableStateOf(existing?.entertainmentWindow ?: "08:00 PM - 08:45 PM")
    }
    var restricted by remember { mutableStateOf(existing?.restrictedInFocus ?: true) }

    AlertDialog(
      onDismissRequest = {
        showAddLimitDialog = false
        editingLimit = null
      },
      title = {
        Text(if (existing == null) "Add Distracting App Limit" else "Edit ${existing.appName} Limit")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          if (existing == null) {
            OutlinedTextField(
              value = appName,
              onValueChange = { appName = it },
              label = { Text("Application Name (e.g., Instagram, YouTube)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = packageName,
              onValueChange = { packageName = it },
              label = { Text("Android Package Name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = limitMinStr,
              onValueChange = { limitMinStr = it.filter { ch -> ch.isDigit() } },
              label = { Text("Daily Limit (mins)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = usedMinStr,
              onValueChange = { usedMinStr = it.filter { ch -> ch.isDigit() } },
              label = { Text("Used Today (mins)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(
            value = window,
            onValueChange = { window = it },
            label = { Text("Allowed Entertainment Window") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSaveAppLimit(
              existing?.id,
              appName.ifBlank { "Entertainment App" },
              packageName.ifBlank { "com.example.app" },
              category,
              limitMinStr.toLongOrNull() ?: 30L,
              usedMinStr.toLongOrNull() ?: 0L,
              window,
              restricted,
            )
            showAddLimitDialog = false
            editingLimit = null
          }
        ) {
          Text("Save Limit")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showAddLimitDialog = false
            editingLimit = null
          }
        ) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun AppLimitCard(
  limit: AppUsageLimit,
  isFocusModeActive: Boolean,
  onEditClick: () -> Unit,
  onToggleRestricted: (Boolean) -> Unit,
) {
  val ratio =
    (limit.usedTodayMinutes.toFloat() / limit.dailyLimitMinutes.coerceAtLeast(1L)).coerceIn(0f, 1f)
  val isApproachingOrReached = ratio >= 0.8f
  val barColor =
    when {
      ratio >= 1f -> Color(0xFFDC2626)
      ratio >= 0.75f -> Color(0xFFD97706)
      else -> MaterialTheme.colorScheme.primary
    }

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
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = limit.appName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isFocusModeActive && limit.restrictedInFocus) {
              Surface(
                color = Color(0xFFDC2626).copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp),
              ) {
                Text(
                  text = "MUTED IN FOCUS",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFFDC2626),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = "${limit.category} • Window: ${limit.entertainmentWindow}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        TextButton(onClick = onEditClick) { Text("Adjust Limit") }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Used: ${limit.usedTodayMinutes}m / ${limit.dailyLimitMinutes}m limit",
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text = "${(limit.dailyLimitMinutes - limit.usedTodayMinutes).coerceAtLeast(0L)}m left",
          style = MaterialTheme.typography.labelMedium,
          color = barColor,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { ratio },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = barColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
      )

      if (isApproachingOrReached) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.WarningAmber,
            contentDescription = null,
            tint = barColor,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (ratio >= 1f) "Daily limit reached! Save entertainment for tomorrow."
              else "Approaching daily limit (>80% used).",
            style = MaterialTheme.typography.bodySmall,
            color = barColor,
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Restrict during Smart Focus sessions",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = limit.restrictedInFocus, onCheckedChange = onToggleRestricted)
      }
    }
  }
}

package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.LocalMobileStudyStore
import com.example.data.StudyBuddyRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.AiStudyAssistantBottomSheet
import com.example.ui.screens.FocusAndDisciplineScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.OnboardingSurveyScreen
import com.example.ui.screens.PlannerAndDeadlinesScreen
import com.example.ui.screens.ProfileAndAnalyticsScreen
import com.example.ui.screens.SyllabusPlannerScreen
import com.example.ui.theme.StudyBuddyTheme
import com.example.viewmodel.DataUiState
import com.example.viewmodel.StudyBuddyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private const val SESSION_PREFS = "study_buddy_mobile_session_prefs"
private const val KEY_VERIFIED_MOBILE = "verified_mobile_number"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      var darkTheme by rememberSaveable { mutableStateOf(false) }
      StudyBuddyTheme(darkTheme = darkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          StudyBuddyRootApp(
            darkTheme = darkTheme,
            onToggleDarkTheme = { darkTheme = !darkTheme },
          )
        }
      }
    }
  }
}

@Composable
fun StudyBuddyRootApp(
  darkTheme: Boolean,
  onToggleDarkTheme: () -> Unit,
) {
  val context = LocalContext.current
  val sessionPrefs = remember {
    context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
  }

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var verifiedMobileSession by remember {
    mutableStateOf(sessionPrefs.getString(KEY_VERIFIED_MOBILE, null))
  }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val firebaseUser = currentUser
  val mobileSession = verifiedMobileSession

  if (firebaseUser == null && mobileSession.isNullOrBlank()) {
    AuthScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
      onMobileOtpVerified = { phone ->
        sessionPrefs.edit().putString(KEY_VERIFIED_MOBILE, phone).apply()
        verifiedMobileSession = phone
      },
    )
  } else {
    val activeUserId = firebaseUser?.uid ?: "mobile_${mobileSession!!.filter { it.isDigit() }}"
    val initialName = firebaseUser?.displayName.orEmpty()
    val isMobileLocalSession = firebaseUser == null

    key(activeUserId) {
      AuthenticatedStudyBuddyApp(
        currentUserId = activeUserId,
        initialUserName = initialName,
        isMobileLocalSession = isMobileLocalSession,
        verifiedMobileNumber = firebaseUser?.phoneNumber ?: mobileSession,
        darkTheme = darkTheme,
        onToggleDarkTheme = onToggleDarkTheme,
        onClearMobileSession = {
          sessionPrefs.edit().remove(KEY_VERIFIED_MOBILE).apply()
          verifiedMobileSession = null
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedStudyBuddyApp(
  currentUserId: String,
  initialUserName: String,
  isMobileLocalSession: Boolean,
  verifiedMobileNumber: String?,
  darkTheme: Boolean,
  onToggleDarkTheme: () -> Unit,
  onClearMobileSession: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: StudyBuddyViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val localStore =
              if (isMobileLocalSession) LocalMobileStudyStore(app, currentUserId) else null
            StudyBuddyViewModel(StudyBuddyRepository(db), currentUserId, localStore)
          }
        },
    )

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
  val topicsState by viewModel.topicsState.collectAsStateWithLifecycle()
  val deadlinesState by viewModel.deadlinesState.collectAsStateWithLifecycle()
  val notesState by viewModel.notesState.collectAsStateWithLifecycle()
  val appLimitsState by viewModel.appLimitsState.collectAsStateWithLifecycle()
  val studyGroupsState by viewModel.studyGroupsState.collectAsStateWithLifecycle()
  val focusState by viewModel.focusState.collectAsStateWithLifecycle()
  val aiState by viewModel.aiState.collectAsStateWithLifecycle()
  val aiPersonalizationTip by viewModel.aiPersonalizationTip.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusBanner) {
    val msg = statusBanner
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusBanner()
    }
  }

  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  var showAiAssistantSheet by rememberSaveable { mutableStateOf(false) }

  BackHandler(enabled = currentTab != 0) {
    currentTab = 0
  }

  when (val pState = profileState) {
    is DataUiState.Loading -> {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Text(
            text = "Loading your Study Buddy workspace...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
          )
        }
      }
    }

    else -> {
      val profile = (pState as? DataUiState.Success)?.data
      if (profile == null || !profile.onboardingCompleted) {
        OnboardingSurveyScreen(
          initialName = initialUserName,
          onCompleteOnboarding = { completedProfile ->
            viewModel.completeOnboardingAndSeedData(completedProfile)
          },
        )
      } else {
        val tasks = (tasksState as? DataUiState.Success)?.data.orEmpty()
        val topics = (topicsState as? DataUiState.Success)?.data.orEmpty()
        val deadlines = (deadlinesState as? DataUiState.Success)?.data.orEmpty()
        val notes = (notesState as? DataUiState.Success)?.data.orEmpty()
        val appLimits = (appLimitsState as? DataUiState.Success)?.data.orEmpty()
        val studyGroups = (studyGroupsState as? DataUiState.Success)?.data.orEmpty()

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            TopAppBar(
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Study Buddy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                }
              },
              actions = {
                IconButton(
                  onClick = { showAiAssistantSheet = true },
                  modifier = Modifier.testTag("top_bar_ai_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Open AI Study Assistant",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = onToggleDarkTheme,
                  modifier = Modifier.testTag("toggle_theme_button"),
                ) {
                  Icon(
                    imageVector =
                      if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle light/dark mode",
                  )
                }
              },
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
          },
          floatingActionButton = {
            ExtendedFloatingActionButton(
              onClick = { showAiAssistantSheet = true },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_ai_assistant"),
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("AI Study Buddy", fontWeight = FontWeight.SemiBold)
            }
          },
          bottomBar = {
            NavigationBar {
              NavigationBarItem(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") },
                modifier = Modifier.testTag("nav_home"),
              )
              NavigationBarItem(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Planner") },
                label = { Text("Planner") },
                modifier = Modifier.testTag("nav_planner"),
              )
              NavigationBarItem(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = "Syllabus") },
                label = { Text("Syllabus") },
                modifier = Modifier.testTag("nav_syllabus"),
              )
              NavigationBarItem(
                selected = currentTab == 3,
                onClick = { currentTab = 3 },
                icon = { Icon(Icons.Default.Timer, contentDescription = "Focus") },
                label = { Text("Focus") },
                modifier = Modifier.testTag("nav_focus"),
              )
              NavigationBarItem(
                selected = currentTab == 4,
                onClick = { currentTab = 4 },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") },
                modifier = Modifier.testTag("nav_profile"),
              )
            }
          },
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
              0 ->
                HomeDashboardScreen(
                  profile = profile,
                  tasks = tasks,
                  topics = topics,
                  deadlines = deadlines,
                  aiPersonalizationTip = aiPersonalizationTip,
                  onStartTaskFocus = { task ->
                    viewModel.startFocusSessionForTask(task)
                    currentTab = 3
                  },
                  onToggleTaskCompleted = { viewModel.toggleTaskCompleted(it) },
                  onDeleteTask = { viewModel.deleteTask(it) },
                  onAddTask = { title, sub, topic, cat, slot, dur, prio ->
                    viewModel.addTask(title, sub, topic, cat, slot, dur, prio)
                  },
                  onRefreshAiInsight = { viewModel.generateAiSmartScheduleAdjustment() },
                  onOpenAiAssistant = { showAiAssistantSheet = true },
                )

              1 ->
                PlannerAndDeadlinesScreen(
                  tasks = tasks,
                  deadlines = deadlines,
                  onAddTask = { title, sub, topic, cat, slot, dur, prio ->
                    viewModel.addTask(title, sub, topic, cat, slot, dur, prio)
                  },
                  onToggleTaskCompleted = { viewModel.toggleTaskCompleted(it) },
                  onRescheduleTask = { task, newSlot ->
                    viewModel.rescheduleMissedTask(task, newSlot)
                  },
                  onDeleteTask = { viewModel.deleteTask(it) },
                  onAddDeadline = { title, sub, type, date, days, prio ->
                    viewModel.addDeadline(title, sub, type, date, days, prio)
                  },
                  onToggleDeadlineCompleted = { viewModel.toggleDeadlineCompleted(it) },
                  onDeleteDeadline = { viewModel.deleteDeadline(it) },
                )

              2 ->
                SyllabusPlannerScreen(
                  topics = topics,
                  onAddTopic = { sub, unit, chap, title, diff ->
                    viewModel.addSyllabusTopic(sub, unit, chap, title, diff)
                  },
                  onCycleTopicStatus = { viewModel.cycleTopicStatus(it) },
                  onDeleteTopic = { viewModel.deleteTopic(it) },
                  onExtractSyllabusWithAi = { rawText, sub ->
                    viewModel.extractSyllabusWithAi(rawText, sub)
                  },
                )

              3 ->
                FocusAndDisciplineScreen(
                  focusState = focusState,
                  appLimits = appLimits,
                  onSelectTimerMode = { mode, mins -> viewModel.selectTimerMode(mode, mins) },
                  onStartOrResumeTimer = { viewModel.startOrResumeTimer() },
                  onPauseTimer = { viewModel.pauseTimer() },
                  onResetTimer = { viewModel.resetTimer() },
                  onCompleteSessionEarly = { viewModel.completeFocusSessionEarly() },
                  onSaveAppLimit = { id, name, pkg, cat, lim, used, win, rest ->
                    viewModel.addOrUpdateAppLimit(id, name, pkg, cat, lim, used, win, rest)
                  },
                  onSyncDeviceUsage = { viewModel.syncDeviceAppUsageStats(context) },
                )

              else ->
                ProfileAndAnalyticsScreen(
                  profile = profile,
                  verifiedMobileNumber = verifiedMobileNumber,
                  tasks = tasks,
                  topics = topics,
                  notes = notes,
                  appLimits = appLimits,
                  studyGroups = studyGroups,
                  onAddNoteOrReflection = { title, sub, content, type ->
                    viewModel.addNoteOrReflection(title, sub, content, type)
                  },
                  onDeleteNote = { viewModel.deleteNote(it) },
                  onCreateStudyGroup = { name, sub, topic ->
                    viewModel.createPeerStudyGroup(name, sub, topic)
                  },
                  onJoinStudyGroup = { viewModel.joinStudyGroup(it) },
                  onUpdateProfile = { viewModel.updateProfile(it) },
                  onSignOut = {
                    onClearMobileSession()
                    signOut(
                      credentialManager = credentialManager,
                      scope = scope,
                    )
                  },
                )
            }
          }
        }

        if (showAiAssistantSheet) {
          AiStudyAssistantBottomSheet(
            aiState = aiState,
            defaultSubject =
              profile.difficultSubjectsCsv.substringBefore(",").trim().ifBlank {
                "Artificial Intelligence"
              },
            onAskAi = { mode, subject, prompt ->
              viewModel.askAiStudyAssistant(mode, subject, prompt)
            },
            onSaveResponseAsNote = { title, subject, content, noteType ->
              viewModel.addNoteOrReflection(title, subject, content, noteType)
            },
            onDismiss = { showAiAssistantSheet = false },
          )
        }
      }
    }
  }
}

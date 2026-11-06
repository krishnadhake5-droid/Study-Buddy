package com.example.viewmodel

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcademicDeadline
import com.example.data.AcademicTask
import com.example.data.AppUsageLimit
import com.example.data.LocalMobileStudyStore
import com.example.data.PeerStudyGroup
import com.example.data.StudentProfile
import com.example.data.StudyBuddyRepository
import com.example.data.StudyNote
import com.example.data.SyllabusTopic
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "StudyBuddyVM"

sealed interface DataUiState<out T> {
  data object Loading : DataUiState<Nothing>
  data class Success<T>(val data: T) : DataUiState<T>
  data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiAssistantState {
  data object Idle : AiAssistantState
  data object Loading : AiAssistantState
  data class Streaming(val text: String) : AiAssistantState
  data class Success(val text: String) : AiAssistantState
  data class Error(val message: String) : AiAssistantState
}

enum class FocusTimerMode(val label: String, val defaultMinutes: Int) {
  FOCUS_SESSION("Study Focus Session", 45),
  SHORT_BREAK("Short Refreshment Break", 10),
  LONG_BREAK("Long Break & Family Time", 25),
  POWER_REST("Afternoon Power Rest", 20),
}

data class FocusSessionState(
  val isRunning: Boolean = false,
  val mode: FocusTimerMode = FocusTimerMode.FOCUS_SESSION,
  val activeSubject: String = "Mathematics",
  val activeTaskTitle: String = "Complete Mathematics Chapter 3",
  val remainingSeconds: Int = 45 * 60,
  val totalSeconds: Int = 45 * 60,
  val completedSummaryMessage: String? = null,
)

class StudyBuddyViewModel(
  private val repository: StudyBuddyRepository,
  private val currentUserId: String,
  private val localStore: LocalMobileStudyStore? = null,
) : ViewModel() {

  private val generalAiModel by lazy {
    Firebase.ai.generativeModel("gemini-3.8-flash")
  }

  private val syllabusExtractionSchema =
    Schema.obj(
      properties =
        mapOf(
          "topics" to
            Schema.array(
              items =
                Schema.obj(
                  properties =
                    mapOf(
                      "subject" to Schema.string("Subject name"),
                      "unitName" to Schema.string("Unit number and name"),
                      "chapterName" to Schema.string("Chapter name"),
                      "topicTitle" to Schema.string("Specific topic title"),
                      "difficulty" to Schema.enumeration(listOf("EASY", "MEDIUM", "HARD")),
                    )
                )
            )
        )
    )

  private val syllabusJsonModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = syllabusExtractionSchema
        },
    )
  }

  // --- Reactive Streams (Firestore when Firebase Auth is active; LocalMobileStudyStore when Mobile OTP session is active) ---
  val profileState: StateFlow<DataUiState<StudentProfile?>> =
    (localStore?.profileFlow ?: repository.observeProfile(currentUserId))
      .map<StudentProfile?, DataUiState<StudentProfile?>> { DataUiState.Success(it) }
      .catch { error -> emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load profile"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  val tasksState: StateFlow<DataUiState<List<AcademicTask>>> =
    (localStore?.tasksFlow ?: repository.observeTasks(currentUserId))
      .map<List<AcademicTask>, DataUiState<List<AcademicTask>>> { DataUiState.Success(it) }
      .catch { error -> emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load tasks"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  val topicsState: StateFlow<DataUiState<List<SyllabusTopic>>> =
    (localStore?.topicsFlow ?: repository.observeTopics(currentUserId))
      .map<List<SyllabusTopic>, DataUiState<List<SyllabusTopic>>> { DataUiState.Success(it) }
      .catch { error -> emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load syllabus"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  val deadlinesState: StateFlow<DataUiState<List<AcademicDeadline>>> =
    (localStore?.deadlinesFlow ?: repository.observeDeadlines(currentUserId))
      .map<List<AcademicDeadline>, DataUiState<List<AcademicDeadline>>> { DataUiState.Success(it) }
      .catch { error -> emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load deadlines"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  val notesState: StateFlow<DataUiState<List<StudyNote>>> =
    (localStore?.notesFlow ?: repository.observeNotes(currentUserId))
      .map<List<StudyNote>, DataUiState<List<StudyNote>>> { DataUiState.Success(it) }
      .catch { error -> emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load notes"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  val appLimitsState: StateFlow<DataUiState<List<AppUsageLimit>>> =
    (localStore?.appLimitsFlow ?: repository.observeAppLimits(currentUserId))
      .map<List<AppUsageLimit>, DataUiState<List<AppUsageLimit>>> { DataUiState.Success(it) }
      .catch { error -> emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load app limits"))) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  val studyGroupsState: StateFlow<DataUiState<List<PeerStudyGroup>>> =
    (localStore?.studyGroupsFlow ?: repository.observeStudyGroups())
      .map<List<PeerStudyGroup>, DataUiState<List<PeerStudyGroup>>> { DataUiState.Success(it) }
      .catch { error ->
        emit(DataUiState.Error(unwrapErrorMsg(error, "Failed to load study groups")))
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

  // --- AI Study Assistant State ---
  private val _aiState = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
  val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

  private val _aiPersonalizationTip =
    MutableStateFlow(
      "AI Insight: Prioritize difficult subjects (AI & Mathematics) in your peak evening focus slot, and keep sessions to 45 minutes with a 10-minute hydration break."
    )
  val aiPersonalizationTip: StateFlow<String> = _aiPersonalizationTip.asStateFlow()

  // --- Focus & Break Timer State ---
  private val _focusState = MutableStateFlow(FocusSessionState())
  val focusState: StateFlow<FocusSessionState> = _focusState.asStateFlow()
  private var timerJob: Job? = null

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  fun clearStatusBanner() {
    _statusBanner.value = null
  }

  private suspend fun saveProfileInternal(profile: StudentProfile, isUpdate: Boolean) {
    if (localStore != null) {
      localStore.saveProfile(profile)
    } else {
      repository.saveProfile(profile, isUpdate = isUpdate)
    }
  }

  private suspend fun createTaskInternal(task: AcademicTask): String {
    return if (localStore != null) localStore.createTask(task) else repository.createTask(task)
  }

  private suspend fun createTopicInternal(topic: SyllabusTopic): String {
    return if (localStore != null) localStore.createTopic(topic) else repository.createTopic(topic)
  }

  private suspend fun createDeadlineInternal(deadline: AcademicDeadline): String {
    return if (localStore != null) localStore.createDeadline(deadline)
    else repository.createDeadline(deadline)
  }

  private suspend fun createNoteInternal(note: StudyNote): String {
    return if (localStore != null) localStore.createNote(note) else repository.createNote(note)
  }

  private suspend fun createAppLimitInternal(limit: AppUsageLimit): String {
    return if (localStore != null) localStore.createAppLimit(limit)
    else repository.createAppLimit(limit)
  }

  private suspend fun createStudyGroupInternal(group: PeerStudyGroup): String {
    return if (localStore != null) localStore.createStudyGroup(group)
    else repository.createStudyGroup(group)
  }

  // --- Onboarding & Realistic Sample Ecosystem Initialization ---
  fun completeOnboardingAndSeedData(profileInput: StudentProfile) {
    viewModelScope.launch {
      try {
        val completedProfile = profileInput.copy(onboardingCompleted = true)
        saveProfileInternal(completedProfile, isUpdate = false)

        val currentTasks = (tasksState.value as? DataUiState.Success)?.data.orEmpty()
        if (currentTasks.isEmpty()) {
          seedPersonalizedRoutineAndGoals(completedProfile)
        }

        val currentTopics = (topicsState.value as? DataUiState.Success)?.data.orEmpty()
        if (currentTopics.isEmpty()) {
          seedDefaultSyllabus()
        }

        val currentDeadlines = (deadlinesState.value as? DataUiState.Success)?.data.orEmpty()
        if (currentDeadlines.isEmpty()) {
          seedDefaultDeadlines()
        }

        val currentLimits = (appLimitsState.value as? DataUiState.Success)?.data.orEmpty()
        if (currentLimits.isEmpty()) {
          seedDefaultAppLimits()
        }

        val currentNotes = (notesState.value as? DataUiState.Success)?.data.orEmpty()
        if (currentNotes.isEmpty()) {
          seedDefaultNotesAndAttendance()
        }

        val currentGroups = (studyGroupsState.value as? DataUiState.Success)?.data.orEmpty()
        if (currentGroups.isEmpty()) {
          createStudyGroupInternal(
            PeerStudyGroup(
              creatorName = completedProfile.studentName.ifBlank { "Student" },
              groupName = "AI & Algorithms Exam Prep Circle",
              subject = "Artificial Intelligence",
              activeTopic = "Unit 2: A* Search & Alpha-Beta Pruning",
              memberCount = 14L,
            )
          )
        }
        _statusBanner.value = "Personalized routine, syllabus, and academic goals generated!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Failed to complete onboarding", e)
        _statusBanner.value = "Saved profile with notice: ${e.localizedMessage}"
      }
    }
  }

  fun updateProfile(profile: StudentProfile) {
    viewModelScope.launch {
      try {
        saveProfileInternal(profile.copy(onboardingCompleted = true), isUpdate = true)
        _statusBanner.value = "Profile & schedule preferences updated!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not update profile: ${e.localizedMessage}"
      }
    }
  }

  private suspend fun seedPersonalizedRoutineAndGoals(profile: StudentProfile) {
    val subjects =
      profile.subjectsCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty { listOf("Artificial Intelligence", "Mathematics", "Operating Systems") }
    val primarySub = subjects.getOrElse(0) { "Artificial Intelligence" }
    val secondarySub = subjects.getOrElse(1) { "Mathematics" }

    val sampleGoals =
      listOf(
        AcademicTask(
          title = "Complete $secondarySub Chapter 3",
          subject = secondarySub,
          chapterOrTopic = "Differential Equations & Matrix Eigenvalues",
          category = "GOAL",
          timeSlot = "05:00 PM - 05:45 PM",
          durationMinutes = profile.sessionLengthMinutes,
          priority = "HIGH",
          completed = true,
        ),
        AcademicTask(
          title = "Revise 2 topics of $primarySub",
          subject = primarySub,
          chapterOrTopic = "Unit 2: BFS, DFS & A* Heuristic Search",
          category = "GOAL",
          timeSlot = "06:00 PM - 06:45 PM",
          durationMinutes = profile.sessionLengthMinutes,
          priority = "HIGH",
          completed = true,
        ),
        AcademicTask(
          title = "Solve 15 practice questions",
          subject = secondarySub,
          chapterOrTopic = "Previous Year Semester Exam Problems",
          category = "GOAL",
          timeSlot = "07:15 PM - 08:00 PM",
          durationMinutes = 45L,
          priority = "HIGH",
          completed = false,
        ),
        AcademicTask(
          title = "Complete one college assignment",
          subject = primarySub,
          chapterOrTopic = "Lab Report: Intelligent Agents Implementation",
          category = "GOAL",
          timeSlot = "08:00 PM - 08:30 PM",
          durationMinutes = 30L,
          priority = "MEDIUM",
          completed = false,
        ),
        AcademicTask(
          title = "Revise yesterday's topics",
          subject = subjects.getOrElse(2) { "Operating Systems" },
          chapterOrTopic = "Process Synchronization & Deadlocks Flashcards",
          category = "GOAL",
          timeSlot = "09:00 PM - 09:45 PM",
          durationMinutes = 45L,
          priority = "MEDIUM",
          completed = true,
        ),
      )

    val sampleRoutine =
      listOf(
        AcademicTask(
          title = "Wake up & morning mindfulness routine",
          subject = "Wellness",
          chapterOrTopic = "Hydration, light stretching & day review",
          category = "ROUTINE",
          timeSlot = profile.wakeUpTime,
          durationMinutes = 45L,
          priority = "MEDIUM",
          completed = true,
        ),
        AcademicTask(
          title = "Breakfast & college preparation",
          subject = "Personal",
          chapterOrTopic = "Nutritious breakfast & packing study notes",
          category = "ROUTINE",
          timeSlot = "08:00 AM - 08:45 AM",
          durationMinutes = 45L,
          priority = "LOW",
          completed = true,
        ),
        AcademicTask(
          title = "College / Academic Lectures",
          subject = "Campus",
          chapterOrTopic = "Attend lectures & take active class notes",
          category = "ROUTINE",
          timeSlot = profile.collegeTiming,
          durationMinutes = 420L,
          priority = "HIGH",
          completed = true,
        ),
        AcademicTask(
          title = "Travel home & refreshment break",
          subject = "Rest & Commute",
          chapterOrTopic = "Decompress with music & healthy snack",
          category = "ROUTINE",
          timeSlot = "04:00 PM - 05:00 PM",
          durationMinutes = profile.travelMinutes.coerceAtLeast(30L),
          priority = "LOW",
          completed = true,
        ),
        AcademicTask(
          title = "Study Session 1: Deep Concept Focus ($secondarySub)",
          subject = secondarySub,
          chapterOrTopic = "High-concentration problem solving",
          category = "ROUTINE",
          timeSlot = "05:00 PM - 05:45 PM",
          durationMinutes = profile.sessionLengthMinutes,
          priority = "HIGH",
          completed = true,
        ),
        AcademicTask(
          title = "Short Refreshment Break",
          subject = "Break",
          chapterOrTopic = "Stretch, drink water, walk around away from screens",
          category = "ROUTINE",
          timeSlot = "05:45 PM - 06:00 PM",
          durationMinutes = profile.breakLengthMinutes,
          priority = "LOW",
          completed = true,
        ),
        AcademicTask(
          title = "Study Session 2: Core Theory ($primarySub)",
          subject = primarySub,
          chapterOrTopic = "Active recall & diagram practice",
          category = "ROUTINE",
          timeSlot = "06:00 PM - 06:45 PM",
          durationMinutes = profile.sessionLengthMinutes,
          priority = "HIGH",
          completed = false,
        ),
        AcademicTask(
          title = if (profile.napPreference) "Power Rest / Afternoon Nap" else "Physical Activity & Walk",
          subject = "Wellness",
          chapterOrTopic = "Recharge mental energy without social media scrolling",
          category = "ROUTINE",
          timeSlot = "06:45 PM - 07:15 PM",
          durationMinutes = 30L,
          priority = "MEDIUM",
          completed = false,
        ),
        AcademicTask(
          title = "Assignments & Practice Questions",
          subject = primarySub,
          chapterOrTopic = "Solve numericals & lab submissions",
          category = "ROUTINE",
          timeSlot = "07:15 PM - 08:00 PM",
          durationMinutes = 45L,
          priority = "HIGH",
          completed = false,
        ),
        AcademicTask(
          title = "Dinner, Family Time & Entertainment Window",
          subject = "Personal",
          chapterOrTopic = "Guilt-free relaxation & family conversation",
          category = "ROUTINE",
          timeSlot = "08:00 PM - 09:00 PM",
          durationMinutes = 60L,
          priority = "LOW",
          completed = false,
        ),
        AcademicTask(
          title = "Evening Revision & Tomorrow's Plan",
          subject = "Revision",
          chapterOrTopic = "15-min spaced repetition & daily reflection",
          category = "ROUTINE",
          timeSlot = "09:00 PM - 10:00 PM",
          durationMinutes = 60L,
          priority = "MEDIUM",
          completed = false,
        ),
      )

    for (task in sampleGoals + sampleRoutine) {
      createTaskInternal(task)
    }
  }

  private suspend fun seedDefaultSyllabus() {
    val defaultTopics =
      listOf(
        SyllabusTopic(
          subject = "Artificial Intelligence",
          unitName = "Unit 1 – Introduction to AI",
          chapterName = "Foundations of AI",
          topicTitle = "Introduction to Artificial Intelligence",
          status = "COMPLETED",
          difficulty = "EASY",
          revisionCount = 2L,
        ),
        SyllabusTopic(
          subject = "Artificial Intelligence",
          unitName = "Unit 1 – Introduction to AI",
          chapterName = "Agents & Environments",
          topicTitle = "Intelligent Agents",
          status = "COMPLETED",
          difficulty = "MEDIUM",
          revisionCount = 1L,
        ),
        SyllabusTopic(
          subject = "Artificial Intelligence",
          unitName = "Unit 1 – Introduction to AI",
          chapterName = "Agents & Environments",
          topicTitle = "Problem-Solving Agents",
          status = "COMPLETED",
          difficulty = "MEDIUM",
          revisionCount = 1L,
        ),
        SyllabusTopic(
          subject = "Artificial Intelligence",
          unitName = "Unit 2 – Searching Algorithms",
          chapterName = "Uninformed Search",
          topicTitle = "BFS & DFS Search Strategies",
          status = "COMPLETED",
          difficulty = "EASY",
          revisionCount = 2L,
        ),
        SyllabusTopic(
          subject = "Artificial Intelligence",
          unitName = "Unit 2 – Searching Algorithms",
          chapterName = "Informed Heuristic Search",
          topicTitle = "A* Algorithm & Admissible Heuristics",
          status = "STUDYING",
          difficulty = "HARD",
          revisionCount = 1L,
        ),
        SyllabusTopic(
          subject = "Artificial Intelligence",
          unitName = "Unit 2 – Searching Algorithms",
          chapterName = "Adversarial Search",
          topicTitle = "Minimax & Alpha-Beta Pruning",
          status = "NEEDS_REVISION",
          difficulty = "HARD",
          revisionCount = 1L,
        ),
        SyllabusTopic(
          subject = "Mathematics",
          unitName = "Unit 1 – Linear Algebra",
          chapterName = "Matrices & Vector Spaces",
          topicTitle = "Eigenvalues and Eigenvectors",
          status = "COMPLETED",
          difficulty = "MEDIUM",
          revisionCount = 2L,
        ),
        SyllabusTopic(
          subject = "Mathematics",
          unitName = "Unit 2 – Multivariable Calculus",
          chapterName = "Chapter 3: Partial Derivatives",
          topicTitle = "Lagrange Multipliers & Gradient Descent",
          status = "STUDYING",
          difficulty = "HARD",
          revisionCount = 0L,
        ),
        SyllabusTopic(
          subject = "Operating Systems",
          unitName = "Unit 1 – Process Management",
          chapterName = "CPU Scheduling & Threads",
          topicTitle = "Preemptive Priority & Multilevel Queue Scheduling",
          status = "COMPLETED",
          difficulty = "MEDIUM",
          revisionCount = 1L,
        ),
        SyllabusTopic(
          subject = "Operating Systems",
          unitName = "Unit 2 – Concurrency",
          chapterName = "Deadlocks",
          topicTitle = "Banker's Algorithm & Resource Allocation Graphs",
          status = "NOT_STARTED",
          difficulty = "HARD",
          revisionCount = 0L,
        ),
      )

    for (topic in defaultTopics) {
      createTopicInternal(topic)
    }
  }

  private suspend fun seedDefaultDeadlines() {
    val items =
      listOf(
        AcademicDeadline(
          title = "End-Semester Examination",
          subject = "All Subjects",
          type = "EXAM",
          dueDateLabel = "2026-12-01",
          daysRemaining = 25L,
          priority = "HIGH",
          completed = false,
        ),
        AcademicDeadline(
          title = "AI Search Heuristics Lab Submission",
          subject = "Artificial Intelligence",
          type = "PRACTICAL",
          dueDateLabel = "2026-11-11",
          daysRemaining = 5L,
          priority = "HIGH",
          completed = false,
        ),
        AcademicDeadline(
          title = "Mathematics Problem Set #3",
          subject = "Mathematics",
          type = "ASSIGNMENT",
          dueDateLabel = "2026-11-09",
          daysRemaining = 3L,
          priority = "HIGH",
          completed = false,
        ),
        AcademicDeadline(
          title = "Operating Systems Kernel Mini-Project",
          subject = "Operating Systems",
          type = "PROJECT",
          dueDateLabel = "2026-11-18",
          daysRemaining = 12L,
          priority = "MEDIUM",
          completed = false,
        ),
      )
    for (item in items) {
      createDeadlineInternal(item)
    }
  }

  private suspend fun seedDefaultAppLimits() {
    val limits =
      listOf(
        AppUsageLimit(
          appName = "Instagram",
          packageName = "com.instagram.android",
          category = "Social Media",
          dailyLimitMinutes = 30L,
          usedTodayMinutes = 22L,
          entertainmentWindow = "08:00 PM - 08:30 PM",
          restrictedInFocus = true,
        ),
        AppUsageLimit(
          appName = "YouTube",
          packageName = "com.google.android.youtube",
          category = "Video & Entertainment",
          dailyLimitMinutes = 45L,
          usedTodayMinutes = 30L,
          entertainmentWindow = "08:00 PM - 08:45 PM",
          restrictedInFocus = true,
        ),
        AppUsageLimit(
          appName = "Mobile Games",
          packageName = "com.supercell.clashofclans",
          category = "Games",
          dailyLimitMinutes = 30L,
          usedTodayMinutes = 15L,
          entertainmentWindow = "08:30 PM - 09:00 PM",
          restrictedInFocus = true,
        ),
        AppUsageLimit(
          appName = "Other Social Media",
          packageName = "com.twitter.android",
          category = "Social Media",
          dailyLimitMinutes = 20L,
          usedTodayMinutes = 10L,
          entertainmentWindow = "08:00 PM - 08:20 PM",
          restrictedInFocus = true,
        ),
      )
    for (limit in limits) {
      createAppLimitInternal(limit)
    }
  }

  private suspend fun seedDefaultNotesAndAttendance() {
    val notes =
      listOf(
        StudyNote(
          title = "A* Search Optimality Conditions",
          subject = "Artificial Intelligence",
          content =
            "• f(n) = g(n) + h(n)\n• Tree-search A* is optimal if h(n) is admissible (never overestimates true cost).\n• Graph-search A* is optimal if h(n) is consistent (monotonic: h(n) <= c(n,a,n') + h(n')).",
          noteType = "NOTE",
        ),
        StudyNote(
          title = "Flashcard: Eigenvalues & Characteristic Equation",
          subject = "Mathematics",
          content =
            "Q: How do you find eigenvalues of matrix A?\nA: Solve det(A - λI) = 0. The roots λ are the eigenvalues, and non-zero solutions v to (A - λI)v = 0 are eigenvectors.",
          noteType = "FLASHCARD",
        ),
        StudyNote(
          title = "Attendance: Artificial Intelligence (36/40 Lectures - 90%)",
          subject = "Artificial Intelligence",
          content = "Attended: 36 | Missed: 4 | Attendance Percentage: 90% (Safe above 75% criteria)",
          noteType = "ATTENDANCE",
        ),
        StudyNote(
          title = "Attendance: Mathematics (33/38 Lectures - 86.8%)",
          subject = "Mathematics",
          content = "Attended: 33 | Missed: 5 | Attendance Percentage: 86.8% (Safe above 75% criteria)",
          noteType = "ATTENDANCE",
        ),
      )
    for (note in notes) {
      createNoteInternal(note)
    }
  }

  // --- Task & Schedule Actions ---
  fun addTask(
    title: String,
    subject: String,
    chapterOrTopic: String,
    category: String,
    timeSlot: String,
    durationMinutes: Long,
    priority: String,
  ) {
    viewModelScope.launch {
      try {
        createTaskInternal(
          AcademicTask(
            title = title,
            subject = subject,
            chapterOrTopic = chapterOrTopic,
            category = category,
            timeSlot = timeSlot,
            durationMinutes = durationMinutes,
            priority = priority,
          )
        )
        _statusBanner.value = "Added '$title' to your $category list."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Failed to add task: ${e.localizedMessage}"
      }
    }
  }

  fun toggleTaskCompleted(task: AcademicTask) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.updateTaskCompletion(task.id, !task.completed)
        } else {
          repository.updateTaskCompletion(task.id, !task.completed)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not update task: ${e.localizedMessage}"
      }
    }
  }

  fun rescheduleMissedTask(task: AcademicTask, newTimeSlot: String) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.rescheduleTask(task.id, newTimeSlot)
        } else {
          repository.rescheduleTask(task.id, newTimeSlot)
        }
        _statusBanner.value = "Rescheduled '${task.title}' to $newTimeSlot without overcrowding!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not reschedule task: ${e.localizedMessage}"
      }
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.deleteTask(taskId)
        } else {
          repository.deleteTask(taskId)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not delete task: ${e.localizedMessage}"
      }
    }
  }

  // --- Syllabus Actions ---
  fun addSyllabusTopic(
    subject: String,
    unitName: String,
    chapterName: String,
    topicTitle: String,
    difficulty: String,
  ) {
    viewModelScope.launch {
      try {
        createTopicInternal(
          SyllabusTopic(
            subject = subject,
            unitName = unitName,
            chapterName = chapterName,
            topicTitle = topicTitle,
            status = "NOT_STARTED",
            difficulty = difficulty,
          )
        )
        _statusBanner.value = "Added syllabus topic '$topicTitle'."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not add topic: ${e.localizedMessage}"
      }
    }
  }

  fun cycleTopicStatus(topic: SyllabusTopic) {
    val nextStatus =
      when (topic.status) {
        "NOT_STARTED" -> "STUDYING"
        "STUDYING" -> "COMPLETED"
        "COMPLETED" -> "NEEDS_REVISION"
        else -> "COMPLETED"
      }
    val nextRevisionCount =
      if (nextStatus == "COMPLETED") topic.revisionCount + 1L else topic.revisionCount
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.updateTopicStatus(topic.id, nextStatus, nextRevisionCount)
        } else {
          repository.updateTopicStatus(topic.id, nextStatus, nextRevisionCount)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not update topic status: ${e.localizedMessage}"
      }
    }
  }

  fun deleteTopic(topicId: String) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.deleteTopic(topicId)
        } else {
          repository.deleteTopic(topicId)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not delete topic: ${e.localizedMessage}"
      }
    }
  }

  // --- Deadlines, Notes, Attendance, Reflections & Peer Groups ---
  fun addDeadline(
    title: String,
    subject: String,
    type: String,
    dueDateLabel: String,
    daysRemaining: Long,
    priority: String,
  ) {
    viewModelScope.launch {
      try {
        createDeadlineInternal(
          AcademicDeadline(
            title = title,
            subject = subject,
            type = type,
            dueDateLabel = dueDateLabel,
            daysRemaining = daysRemaining,
            priority = priority,
          )
        )
        _statusBanner.value = "Added deadline '$title'."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not add deadline: ${e.localizedMessage}"
      }
    }
  }

  fun toggleDeadlineCompleted(deadline: AcademicDeadline) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.toggleDeadlineCompleted(deadline.id, !deadline.completed)
        } else {
          repository.toggleDeadlineCompleted(deadline.id, !deadline.completed)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not update deadline: ${e.localizedMessage}"
      }
    }
  }

  fun deleteDeadline(deadlineId: String) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.deleteDeadline(deadlineId)
        } else {
          repository.deleteDeadline(deadlineId)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not delete deadline: ${e.localizedMessage}"
      }
    }
  }

  fun addNoteOrReflection(title: String, subject: String, content: String, noteType: String) {
    viewModelScope.launch {
      try {
        createNoteInternal(
          StudyNote(
            title = title,
            subject = subject,
            content = content,
            noteType = noteType,
          )
        )
        _statusBanner.value = "Saved $noteType entry '$title'."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not save entry: ${e.localizedMessage}"
      }
    }
  }

  fun deleteNote(noteId: String) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.deleteNote(noteId)
        } else {
          repository.deleteNote(noteId)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not delete note: ${e.localizedMessage}"
      }
    }
  }

  fun addOrUpdateAppLimit(
    limitId: String?,
    appName: String,
    packageName: String,
    category: String,
    dailyLimitMinutes: Long,
    usedTodayMinutes: Long,
    entertainmentWindow: String,
    restrictedInFocus: Boolean,
  ) {
    viewModelScope.launch {
      try {
        if (limitId == null) {
          createAppLimitInternal(
            AppUsageLimit(
              appName = appName,
              packageName = packageName,
              category = category,
              dailyLimitMinutes = dailyLimitMinutes,
              usedTodayMinutes = usedTodayMinutes,
              entertainmentWindow = entertainmentWindow,
              restrictedInFocus = restrictedInFocus,
            )
          )
        } else if (localStore != null) {
          localStore.updateAppLimit(
            limitId = limitId,
            dailyLimitMinutes = dailyLimitMinutes,
            usedTodayMinutes = usedTodayMinutes,
            restrictedInFocus = restrictedInFocus,
            entertainmentWindow = entertainmentWindow,
          )
        } else {
          repository.updateAppLimit(
            limitId = limitId,
            dailyLimitMinutes = dailyLimitMinutes,
            usedTodayMinutes = usedTodayMinutes,
            restrictedInFocus = restrictedInFocus,
            entertainmentWindow = entertainmentWindow,
          )
        }
        _statusBanner.value = "Updated Digital Discipline limit for $appName."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not save app limit: ${e.localizedMessage}"
      }
    }
  }

  fun syncDeviceAppUsageStats(context: Context) {
    viewModelScope.launch {
      try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode =
          appOps?.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
          )
        if (mode != AppOpsManager.MODE_ALLOWED) {
          _statusBanner.value =
            "Grant Usage Access permission in Android Settings to sync live device screen time."
          return@launch
        }
        val usageStatsManager =
          context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@launch
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24L * 60L * 60L * 1000L
        val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val limits = (appLimitsState.value as? DataUiState.Success)?.data.orEmpty()
        var updatedCount = 0
        for (limit in limits) {
          val usage = statsMap[limit.packageName]
          if (usage != null) {
            val minutes = (usage.totalTimeInForeground / 60000L).coerceIn(0L, 1440L)
            if (localStore != null) {
              localStore.updateAppLimit(
                limitId = limit.id,
                dailyLimitMinutes = limit.dailyLimitMinutes,
                usedTodayMinutes = minutes,
                restrictedInFocus = limit.restrictedInFocus,
                entertainmentWindow = limit.entertainmentWindow,
              )
            } else {
              repository.updateAppLimit(
                limitId = limit.id,
                dailyLimitMinutes = limit.dailyLimitMinutes,
                usedTodayMinutes = minutes,
                restrictedInFocus = limit.restrictedInFocus,
                entertainmentWindow = limit.entertainmentWindow,
              )
            }
            updatedCount++
          }
        }
        _statusBanner.value =
          if (updatedCount > 0) "Synced real device usage for $updatedCount apps!"
          else "Usage access active! No additional foreground usage detected for tracked packages."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Error syncing device usage stats", e)
      }
    }
  }

  fun createPeerStudyGroup(groupName: String, subject: String, activeTopic: String) {
    val profile = (profileState.value as? DataUiState.Success)?.data
    val creatorName = profile?.studentName?.ifBlank { "Student" } ?: "Student"
    viewModelScope.launch {
      try {
        createStudyGroupInternal(
          PeerStudyGroup(
            creatorName = creatorName,
            groupName = groupName,
            subject = subject,
            activeTopic = activeTopic,
            memberCount = 1L,
          )
        )
        _statusBanner.value = "Created study group '$groupName'!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not create study group: ${e.localizedMessage}"
      }
    }
  }

  fun joinStudyGroup(group: PeerStudyGroup) {
    viewModelScope.launch {
      try {
        if (localStore != null) {
          localStore.joinStudyGroup(group.id, group.memberCount, group.activeTopic)
        } else {
          repository.joinStudyGroup(group.id, group.memberCount, group.activeTopic)
        }
        _statusBanner.value = "Joined '${group.groupName}'!"
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _statusBanner.value = "Could not join group: ${e.localizedMessage}"
      }
    }
  }

  // --- Smart Focus & Break Timer Controls ---
  fun startFocusSessionForTask(task: AcademicTask) {
    val durationMin = task.durationMinutes.toInt().coerceIn(5, 180)
    _focusState.value =
      FocusSessionState(
        isRunning = false,
        mode = FocusTimerMode.FOCUS_SESSION,
        activeSubject = task.subject,
        activeTaskTitle = task.title,
        remainingSeconds = durationMin * 60,
        totalSeconds = durationMin * 60,
        completedSummaryMessage = null,
      )
    startOrResumeTimer()
  }

  fun selectTimerMode(mode: FocusTimerMode, customMinutes: Int = mode.defaultMinutes) {
    timerJob?.cancel()
    val secs = customMinutes.coerceIn(1, 180) * 60
    _focusState.value =
      _focusState.value.copy(
        isRunning = false,
        mode = mode,
        remainingSeconds = secs,
        totalSeconds = secs,
        completedSummaryMessage = null,
      )
  }

  fun startOrResumeTimer() {
    if (_focusState.value.isRunning) return
    _focusState.value = _focusState.value.copy(isRunning = true, completedSummaryMessage = null)
    timerJob?.cancel()
    timerJob =
      viewModelScope.launch {
        while (_focusState.value.remainingSeconds > 0 && _focusState.value.isRunning) {
          delay(1000L)
          val next = (_focusState.value.remainingSeconds - 1).coerceAtLeast(0)
          _focusState.value = _focusState.value.copy(remainingSeconds = next)
        }
        if (_focusState.value.remainingSeconds == 0 && _focusState.value.isRunning) {
          onTimerFinished()
        }
      }
  }

  fun pauseTimer() {
    timerJob?.cancel()
    _focusState.value = _focusState.value.copy(isRunning = false)
  }

  fun resetTimer() {
    timerJob?.cancel()
    val total = _focusState.value.totalSeconds
    _focusState.value =
      _focusState.value.copy(
        isRunning = false,
        remainingSeconds = total,
        completedSummaryMessage = null,
      )
  }

  fun completeFocusSessionEarly() {
    timerJob?.cancel()
    onTimerFinished()
  }

  private fun onTimerFinished() {
    val current = _focusState.value
    val studiedMinutes = (current.totalSeconds / 60).coerceAtLeast(1)
    if (current.mode == FocusTimerMode.FOCUS_SESSION) {
      val summary =
        "Great work! You completed $studiedMinutes minutes of studying ${current.activeSubject}. Take a 10-minute refreshment break!"
      _focusState.value =
        current.copy(
          isRunning = false,
          mode = FocusTimerMode.SHORT_BREAK,
          remainingSeconds = 10 * 60,
          totalSeconds = 10 * 60,
          completedSummaryMessage = summary,
        )
      val profile = (profileState.value as? DataUiState.Success)?.data
      if (profile != null) {
        viewModelScope.launch {
          try {
            saveProfileInternal(
              profile.copy(
                totalFocusMinutes = profile.totalFocusMinutes + studiedMinutes,
                focusSessionsCompleted = profile.focusSessionsCompleted + 1L,
              ),
              isUpdate = true,
            )
          } catch (e: Exception) {
            Log.w(TAG, "Could not update focus stats", e)
          }
        }
      }
    } else {
      val summary =
        "Break complete! Ready to begin your next focused study session on ${current.activeSubject}?"
      _focusState.value =
        current.copy(
          isRunning = false,
          mode = FocusTimerMode.FOCUS_SESSION,
          remainingSeconds = 45 * 60,
          totalSeconds = 45 * 60,
          completedSummaryMessage = summary,
        )
    }
  }

  // --- Firebase AI Logic: AI Study Assistant ---
  fun askAiStudyAssistant(
    learningMode: String,
    subject: String,
    userPrompt: String,
  ) {
    if (userPrompt.isBlank()) return
    val profile = (profileState.value as? DataUiState.Success)?.data
    val level = profile?.gradeOrDegree ?: "Undergraduate Student"
    val lang = profile?.preferredLanguage ?: "English"

    val modeInstruction =
      when (learningMode) {
        "UNDERSTAND" ->
          "Explain the concept in clear, simple language with relatable real-life examples, step-by-step intuition, and key takeaways."
        "PRACTICE" ->
          "Generate 4 high-yield practice questions (from foundational to exam-level) on this topic, followed by clear step-by-step worked answers and evaluation tips."
        "REVISION" ->
          "Create crisp bulleted revision notes, 4 Q&A flashcards, and a 3-question quick self-check quiz."
        else -> "Provide a clear, helpful academic response."
      }

    val fullPrompt =
      """
      You are Study Buddy, a supportive AI study companion for a student in $level studying $subject (Preferred language: $lang).
      Learning Mode: $learningMode
      Task: $modeInstruction
      Student Request: $userPrompt
      """.trimIndent()

    viewModelScope.launch {
      _aiState.value = AiAssistantState.Loading
      val accumulatedText = StringBuilder()
      try {
        withTimeout(60_000L) {
          generalAiModel.generateContentStream(fullPrompt).collect { chunk ->
            val chunkText = chunk.text.orEmpty()
            accumulatedText.append(chunkText)
            _aiState.value = AiAssistantState.Streaming(accumulatedText.toString())
          }
        }
        if (accumulatedText.isNotBlank()) {
          _aiState.value = AiAssistantState.Success(accumulatedText.toString())
        } else {
          _aiState.value = AiAssistantState.Error("Model returned an empty response.")
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI Assistant streaming timed out", e)
        _aiState.value =
          AiAssistantState.Error("Generation timed out after 60 seconds. Please try again.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Firebase AI generation failed", e)
        _aiState.value = AiAssistantState.Error(e.localizedMessage ?: "AI request failed.")
      }
    }
  }

  // --- Firebase AI Logic: Extract Syllabus Topics & Auto-Distribute ---
  fun extractSyllabusWithAi(rawSyllabusText: String, defaultSubject: String) {
    if (rawSyllabusText.isBlank()) return
    viewModelScope.launch {
      _statusBanner.value = "AI is analyzing your syllabus and extracting structured topics..."
      try {
        val prompt =
          """
          Extract a structured list of academic syllabus topics from the following syllabus text for subject '$defaultSubject'.
          Divide chapters into manageable learning topics and classify difficulty as EASY, MEDIUM, or HARD.
          Syllabus text:
          $rawSyllabusText
          """.trimIndent()

        val response = withTimeout(60_000L) { syllabusJsonModel.generateContent(prompt) }
        val jsonText = response.text ?: throw IllegalStateException("Empty AI JSON response")
        val root = JSONObject(jsonText)
        val array = root.getJSONArray("topics")
        var count = 0
        for (i in 0 until array.length()) {
          val item = array.getJSONObject(i)
          createTopicInternal(
            SyllabusTopic(
              subject = item.optString("subject", defaultSubject).ifBlank { defaultSubject },
              unitName = item.optString("unitName", "Unit 1"),
              chapterName = item.optString("chapterName", "Chapter 1"),
              topicTitle = item.optString("topicTitle", "Topic ${i + 1}"),
              status = "NOT_STARTED",
              difficulty = item.optString("difficulty", "MEDIUM"),
            )
          )
          count++
        }
        _statusBanner.value = "AI extracted and scheduled $count syllabus topics!"
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Syllabus extraction timed out", e)
        _statusBanner.value = "AI syllabus extraction timed out. Please try a shorter excerpt."
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Syllabus extraction failed", e)
        _statusBanner.value = "AI syllabus extraction error: ${e.localizedMessage}"
      }
    }
  }

  // --- Firebase AI Logic: Smart AI Personalization & Adaptive Schedule Adjustment ---
  fun generateAiSmartScheduleAdjustment() {
    val profile = (profileState.value as? DataUiState.Success)?.data ?: return
    val tasks = (tasksState.value as? DataUiState.Success)?.data.orEmpty()
    val topics = (topicsState.value as? DataUiState.Success)?.data.orEmpty()
    val pendingTasks = tasks.filter { !it.completed && it.category == "GOAL" }
    val hardTopics = topics.filter { it.status != "COMPLETED" && it.difficulty == "HARD" }

    viewModelScope.launch {
      try {
        val prompt =
          """
          Student: ${profile.studentName} (${profile.gradeOrDegree})
          Difficult Subjects: ${profile.difficultSubjectsCsv}
          Exam Date: ${profile.examStartDate}
          Pending Daily Goals (${pendingTasks.size}): ${pendingTasks.joinToString { "${it.subject}: ${it.title}" }}
          Pending Hard Syllabus Topics (${hardTopics.size}): ${hardTopics.take(4).joinToString { "${it.subject} - ${it.topicTitle}" }}

          In 2-3 supportive, practical sentences, give a personalized schedule recommendation (e.g. splitting difficult topics into smaller 30-min sessions, using free slots for revision, and maintaining rest balance).
          """.trimIndent()

        val response = withTimeout(60_000L) { generalAiModel.generateContent(prompt) }
        val text = response.text?.trim()
        if (!text.isNullOrBlank()) {
          _aiPersonalizationTip.value = text
          _statusBanner.value = "AI Personalization Engine updated your study recommendations!"
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI personalization recommendation failed", e)
        _statusBanner.value = "Could not refresh AI insight: ${e.localizedMessage}"
      }
    }
  }

  private fun unwrapErrorMsg(error: Throwable, fallback: String): String {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, fallback, rootError)
    return rootError.message ?: fallback
  }
}

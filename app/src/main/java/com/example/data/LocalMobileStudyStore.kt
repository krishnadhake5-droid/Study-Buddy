package com.example.data

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent device storage for students authenticating via Mobile Number + OTP
 * when Firebase Auth's carrier SMS provider is not active in the emulator environment.
 * Ensures zero unauthenticated Firestore requests are ever made while providing full
 * persistence across app restarts for all Study Buddy entities.
 */
class LocalMobileStudyStore(context: Context, private val mobileKey: String) {

  private val prefs =
    context.applicationContext.getSharedPreferences(
      "study_buddy_mobile_store_${mobileKey.filter { it.isLetterOrDigit() }}",
      Context.MODE_PRIVATE,
    )

  private val _profileFlow = MutableStateFlow(loadProfile())
  val profileFlow: Flow<StudentProfile?> = _profileFlow.asStateFlow()

  private val _tasksFlow = MutableStateFlow(loadTasks())
  val tasksFlow: Flow<List<AcademicTask>> = _tasksFlow.asStateFlow()

  private val _topicsFlow = MutableStateFlow(loadTopics())
  val topicsFlow: Flow<List<SyllabusTopic>> = _topicsFlow.asStateFlow()

  private val _deadlinesFlow = MutableStateFlow(loadDeadlines())
  val deadlinesFlow: Flow<List<AcademicDeadline>> = _deadlinesFlow.asStateFlow()

  private val _notesFlow = MutableStateFlow(loadNotes())
  val notesFlow: Flow<List<StudyNote>> = _notesFlow.asStateFlow()

  private val _appLimitsFlow = MutableStateFlow(loadAppLimits())
  val appLimitsFlow: Flow<List<AppUsageLimit>> = _appLimitsFlow.asStateFlow()

  private val _studyGroupsFlow = MutableStateFlow(loadStudyGroups())
  val studyGroupsFlow: Flow<List<PeerStudyGroup>> = _studyGroupsFlow.asStateFlow()

  fun saveProfile(profile: StudentProfile) {
    val updated = profile.copy(id = mobileKey, userId = mobileKey)
    val obj =
      JSONObject().apply {
        put("id", updated.id)
        put("userId", updated.userId)
        put("studentName", updated.studentName)
        put("gradeOrDegree", updated.gradeOrDegree)
        put("institutionName", updated.institutionName)
        put("streamOrBranch", updated.streamOrBranch)
        put("semester", updated.semester)
        put("subjectsCsv", updated.subjectsCsv)
        put("difficultSubjectsCsv", updated.difficultSubjectsCsv)
        put("examStartDate", updated.examStartDate)
        put("preferredLanguage", updated.preferredLanguage)
        put("wakeUpTime", updated.wakeUpTime)
        put("sleepTime", updated.sleepTime)
        put("collegeTiming", updated.collegeTiming)
        put("travelMinutes", updated.travelMinutes)
        put("hasCoaching", updated.hasCoaching)
        put("dailyStudyHours", updated.dailyStudyHours)
        put("preferredStudySlot", updated.preferredStudySlot)
        put("socialMediaMinutes", updated.socialMediaMinutes)
        put("sessionLengthMinutes", updated.sessionLengthMinutes)
        put("breakLengthMinutes", updated.breakLengthMinutes)
        put("napPreference", updated.napPreference)
        put("onboardingCompleted", updated.onboardingCompleted)
        put("streakDays", updated.streakDays)
        put("totalFocusMinutes", updated.totalFocusMinutes)
        put("focusSessionsCompleted", updated.focusSessionsCompleted)
      }
    prefs.edit().putString("profile", obj.toString()).apply()
    _profileFlow.value = updated
  }

  private fun loadProfile(): StudentProfile? {
    val raw = prefs.getString("profile", null) ?: return null
    return try {
      val obj = JSONObject(raw)
      StudentProfile(
        id = obj.optString("id", mobileKey),
        userId = obj.optString("userId", mobileKey),
        studentName = obj.optString("studentName", "Student"),
        gradeOrDegree = obj.optString("gradeOrDegree", "B.Tech Computer Science"),
        institutionName = obj.optString("institutionName", ""),
        streamOrBranch = obj.optString("streamOrBranch", ""),
        semester = obj.optString("semester", "Semester 5"),
        subjectsCsv = obj.optString("subjectsCsv", "Artificial Intelligence, Mathematics"),
        difficultSubjectsCsv = obj.optString("difficultSubjectsCsv", "Artificial Intelligence"),
        examStartDate = obj.optString("examStartDate", "2026-12-01"),
        preferredLanguage = obj.optString("preferredLanguage", "English"),
        wakeUpTime = obj.optString("wakeUpTime", "06:30 AM"),
        sleepTime = obj.optString("sleepTime", "10:30 PM"),
        collegeTiming = obj.optString("collegeTiming", "09:00 AM - 04:00 PM"),
        travelMinutes = obj.optLong("travelMinutes", 45L),
        hasCoaching = obj.optBoolean("hasCoaching", false),
        dailyStudyHours = obj.optLong("dailyStudyHours", 4L),
        preferredStudySlot = obj.optString("preferredStudySlot", "EVENING"),
        socialMediaMinutes = obj.optLong("socialMediaMinutes", 90L),
        sessionLengthMinutes = obj.optLong("sessionLengthMinutes", 45L),
        breakLengthMinutes = obj.optLong("breakLengthMinutes", 10L),
        napPreference = obj.optBoolean("napPreference", true),
        onboardingCompleted = obj.optBoolean("onboardingCompleted", false),
        streakDays = obj.optLong("streakDays", 5L),
        totalFocusMinutes = obj.optLong("totalFocusMinutes", 180L),
        focusSessionsCompleted = obj.optLong("focusSessionsCompleted", 4L),
      )
    } catch (_: Exception) {
      null
    }
  }

  // --- Tasks ---
  fun createTask(task: AcademicTask): String {
    val id = task.id.ifBlank { UUID.randomUUID().toString() }
    val newTask = task.copy(id = id, userId = mobileKey)
    val list = _tasksFlow.value + newTask
    saveTasks(list)
    return id
  }

  fun updateTaskCompletion(taskId: String, completed: Boolean) {
    val list = _tasksFlow.value.map { if (it.id == taskId) it.copy(completed = completed) else it }
    saveTasks(list)
  }

  fun rescheduleTask(taskId: String, newTimeSlot: String) {
    val list = _tasksFlow.value.map { if (it.id == taskId) it.copy(timeSlot = newTimeSlot) else it }
    saveTasks(list)
  }

  fun deleteTask(taskId: String) {
    val list = _tasksFlow.value.filterNot { it.id == taskId }
    saveTasks(list)
  }

  private fun saveTasks(list: List<AcademicTask>) {
    val arr = JSONArray()
    list.forEach { t ->
      arr.put(
        JSONObject().apply {
          put("id", t.id)
          put("userId", mobileKey)
          put("title", t.title)
          put("subject", t.subject)
          put("chapterOrTopic", t.chapterOrTopic)
          put("category", t.category)
          put("timeSlot", t.timeSlot)
          put("durationMinutes", t.durationMinutes)
          put("priority", t.priority)
          put("completed", t.completed)
        }
      )
    }
    prefs.edit().putString("tasks", arr.toString()).apply()
    _tasksFlow.value = list
  }

  private fun loadTasks(): List<AcademicTask> {
    val raw = prefs.getString("tasks", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        AcademicTask(
          id = o.optString("id"),
          userId = o.optString("userId", mobileKey),
          title = o.optString("title"),
          subject = o.optString("subject"),
          chapterOrTopic = o.optString("chapterOrTopic"),
          category = o.optString("category", "GOAL"),
          timeSlot = o.optString("timeSlot"),
          durationMinutes = o.optLong("durationMinutes", 45L),
          priority = o.optString("priority", "HIGH"),
          completed = o.optBoolean("completed", false),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // --- Topics ---
  fun createTopic(topic: SyllabusTopic): String {
    val id = topic.id.ifBlank { UUID.randomUUID().toString() }
    val newTopic = topic.copy(id = id, userId = mobileKey)
    val list = _topicsFlow.value + newTopic
    saveTopics(list)
    return id
  }

  fun updateTopicStatus(topicId: String, newStatus: String, newRevisionCount: Long) {
    val list =
      _topicsFlow.value.map {
        if (it.id == topicId) it.copy(status = newStatus, revisionCount = newRevisionCount) else it
      }
    saveTopics(list)
  }

  fun deleteTopic(topicId: String) {
    saveTopics(_topicsFlow.value.filterNot { it.id == topicId })
  }

  private fun saveTopics(list: List<SyllabusTopic>) {
    val arr = JSONArray()
    list.forEach { t ->
      arr.put(
        JSONObject().apply {
          put("id", t.id)
          put("userId", mobileKey)
          put("subject", t.subject)
          put("unitName", t.unitName)
          put("chapterName", t.chapterName)
          put("topicTitle", t.topicTitle)
          put("status", t.status)
          put("difficulty", t.difficulty)
          put("revisionCount", t.revisionCount)
        }
      )
    }
    prefs.edit().putString("topics", arr.toString()).apply()
    _topicsFlow.value = list
  }

  private fun loadTopics(): List<SyllabusTopic> {
    val raw = prefs.getString("topics", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        SyllabusTopic(
          id = o.optString("id"),
          userId = o.optString("userId", mobileKey),
          subject = o.optString("subject"),
          unitName = o.optString("unitName"),
          chapterName = o.optString("chapterName"),
          topicTitle = o.optString("topicTitle"),
          status = o.optString("status", "NOT_STARTED"),
          difficulty = o.optString("difficulty", "MEDIUM"),
          revisionCount = o.optLong("revisionCount", 0L),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // --- Deadlines ---
  fun createDeadline(deadline: AcademicDeadline): String {
    val id = deadline.id.ifBlank { UUID.randomUUID().toString() }
    val list = _deadlinesFlow.value + deadline.copy(id = id, userId = mobileKey)
    saveDeadlines(list)
    return id
  }

  fun toggleDeadlineCompleted(deadlineId: String, completed: Boolean) {
    saveDeadlines(
      _deadlinesFlow.value.map { if (it.id == deadlineId) it.copy(completed = completed) else it }
    )
  }

  fun deleteDeadline(deadlineId: String) {
    saveDeadlines(_deadlinesFlow.value.filterNot { it.id == deadlineId })
  }

  private fun saveDeadlines(list: List<AcademicDeadline>) {
    val arr = JSONArray()
    list.forEach { d ->
      arr.put(
        JSONObject().apply {
          put("id", d.id)
          put("userId", mobileKey)
          put("title", d.title)
          put("subject", d.subject)
          put("type", d.type)
          put("dueDateLabel", d.dueDateLabel)
          put("daysRemaining", d.daysRemaining)
          put("priority", d.priority)
          put("completed", d.completed)
        }
      )
    }
    prefs.edit().putString("deadlines", arr.toString()).apply()
    _deadlinesFlow.value = list
  }

  private fun loadDeadlines(): List<AcademicDeadline> {
    val raw = prefs.getString("deadlines", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        AcademicDeadline(
          id = o.optString("id"),
          userId = o.optString("userId", mobileKey),
          title = o.optString("title"),
          subject = o.optString("subject"),
          type = o.optString("type", "EXAM"),
          dueDateLabel = o.optString("dueDateLabel"),
          daysRemaining = o.optLong("daysRemaining", 10L),
          priority = o.optString("priority", "HIGH"),
          completed = o.optBoolean("completed", false),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // --- Notes ---
  fun createNote(note: StudyNote): String {
    val id = note.id.ifBlank { UUID.randomUUID().toString() }
    val list = _notesFlow.value + note.copy(id = id, userId = mobileKey)
    saveNotes(list)
    return id
  }

  fun deleteNote(noteId: String) {
    saveNotes(_notesFlow.value.filterNot { it.id == noteId })
  }

  private fun saveNotes(list: List<StudyNote>) {
    val arr = JSONArray()
    list.forEach { n ->
      arr.put(
        JSONObject().apply {
          put("id", n.id)
          put("userId", mobileKey)
          put("title", n.title)
          put("subject", n.subject)
          put("content", n.content)
          put("noteType", n.noteType)
        }
      )
    }
    prefs.edit().putString("notes", arr.toString()).apply()
    _notesFlow.value = list
  }

  private fun loadNotes(): List<StudyNote> {
    val raw = prefs.getString("notes", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        StudyNote(
          id = o.optString("id"),
          userId = o.optString("userId", mobileKey),
          title = o.optString("title"),
          subject = o.optString("subject"),
          content = o.optString("content"),
          noteType = o.optString("noteType", "NOTE"),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // --- App Limits ---
  fun createAppLimit(limit: AppUsageLimit): String {
    val id = limit.id.ifBlank { UUID.randomUUID().toString() }
    val list = _appLimitsFlow.value + limit.copy(id = id, userId = mobileKey)
    saveAppLimits(list)
    return id
  }

  fun updateAppLimit(
    limitId: String,
    dailyLimitMinutes: Long,
    usedTodayMinutes: Long,
    restrictedInFocus: Boolean,
    entertainmentWindow: String,
  ) {
    val list =
      _appLimitsFlow.value.map {
        if (it.id == limitId) {
          it.copy(
            dailyLimitMinutes = dailyLimitMinutes,
            usedTodayMinutes = usedTodayMinutes,
            restrictedInFocus = restrictedInFocus,
            entertainmentWindow = entertainmentWindow,
          )
        } else it
      }
    saveAppLimits(list)
  }

  private fun saveAppLimits(list: List<AppUsageLimit>) {
    val arr = JSONArray()
    list.forEach { l ->
      arr.put(
        JSONObject().apply {
          put("id", l.id)
          put("userId", mobileKey)
          put("appName", l.appName)
          put("packageName", l.packageName)
          put("category", l.category)
          put("dailyLimitMinutes", l.dailyLimitMinutes)
          put("usedTodayMinutes", l.usedTodayMinutes)
          put("entertainmentWindow", l.entertainmentWindow)
          put("restrictedInFocus", l.restrictedInFocus)
        }
      )
    }
    prefs.edit().putString("app_limits", arr.toString()).apply()
    _appLimitsFlow.value = list
  }

  private fun loadAppLimits(): List<AppUsageLimit> {
    val raw = prefs.getString("app_limits", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        AppUsageLimit(
          id = o.optString("id"),
          userId = o.optString("userId", mobileKey),
          appName = o.optString("appName"),
          packageName = o.optString("packageName"),
          category = o.optString("category"),
          dailyLimitMinutes = o.optLong("dailyLimitMinutes", 30L),
          usedTodayMinutes = o.optLong("usedTodayMinutes", 15L),
          entertainmentWindow = o.optString("entertainmentWindow", "08:00 PM - 08:45 PM"),
          restrictedInFocus = o.optBoolean("restrictedInFocus", true),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  // --- Peer Study Groups ---
  fun createStudyGroup(group: PeerStudyGroup): String {
    val id = group.id.ifBlank { UUID.randomUUID().toString() }
    val list = _studyGroupsFlow.value + group.copy(id = id, creatorId = mobileKey)
    saveStudyGroups(list)
    return id
  }

  fun joinStudyGroup(groupId: String, currentMembers: Long, activeTopic: String) {
    val list =
      _studyGroupsFlow.value.map {
        if (it.id == groupId) it.copy(memberCount = currentMembers + 1L, activeTopic = activeTopic)
        else it
      }
    saveStudyGroups(list)
  }

  private fun saveStudyGroups(list: List<PeerStudyGroup>) {
    val arr = JSONArray()
    list.forEach { g ->
      arr.put(
        JSONObject().apply {
          put("id", g.id)
          put("creatorId", g.creatorId)
          put("creatorName", g.creatorName)
          put("groupName", g.groupName)
          put("subject", g.subject)
          put("activeTopic", g.activeTopic)
          put("memberCount", g.memberCount)
          put("visibility", g.visibility)
        }
      )
    }
    prefs.edit().putString("study_groups", arr.toString()).apply()
    _studyGroupsFlow.value = list
  }

  private fun loadStudyGroups(): List<PeerStudyGroup> {
    val raw = prefs.getString("study_groups", null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        PeerStudyGroup(
          id = o.optString("id"),
          creatorId = o.optString("creatorId", mobileKey),
          creatorName = o.optString("creatorName", "Student"),
          groupName = o.optString("groupName"),
          subject = o.optString("subject"),
          activeTopic = o.optString("activeTopic"),
          memberCount = o.optLong("memberCount", 1L),
          visibility = o.optString("visibility", "PUBLIC"),
        )
      }
    } catch (_: Exception) {
      emptyList()
    }
  }
}

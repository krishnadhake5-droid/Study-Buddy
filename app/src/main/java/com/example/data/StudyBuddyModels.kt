package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudentProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val studentName: String = "",
  val gradeOrDegree: String = "B.Tech Computer Science (3rd Year)",
  val institutionName: String = "National Institute of Technology",
  val streamOrBranch: String = "Computer Science & AI",
  val semester: String = "Semester 5",
  val subjectsCsv: String =
    "Artificial Intelligence, Mathematics, Operating Systems, Database Systems, Computer Networks",
  val difficultSubjectsCsv: String = "Artificial Intelligence, Mathematics",
  val examStartDate: String = "2026-12-01",
  val preferredLanguage: String = "English",
  val wakeUpTime: String = "06:30 AM",
  val sleepTime: String = "10:30 PM",
  val collegeTiming: String = "09:00 AM - 04:00 PM",
  val travelMinutes: Long = 45L,
  val hasCoaching: Boolean = false,
  val dailyStudyHours: Long = 4L,
  val preferredStudySlot: String = "EVENING",
  val socialMediaMinutes: Long = 90L,
  val sessionLengthMinutes: Long = 45L,
  val breakLengthMinutes: Long = 10L,
  val napPreference: Boolean = true,
  val onboardingCompleted: Boolean = false,
  val streakDays: Long = 5L,
  val totalFocusMinutes: Long = 180L,
  val focusSessionsCompleted: Long = 4L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "studentName" to studentName.ifBlank { "Student" },
      "gradeOrDegree" to gradeOrDegree.ifBlank { "Undergraduate" },
      "institutionName" to institutionName,
      "streamOrBranch" to streamOrBranch,
      "semester" to semester,
      "subjectsCsv" to subjectsCsv.ifBlank { "Mathematics, Computer Science" },
      "difficultSubjectsCsv" to difficultSubjectsCsv,
      "examStartDate" to examStartDate.ifBlank { "2026-12-01" },
      "preferredLanguage" to preferredLanguage.ifBlank { "English" },
      "wakeUpTime" to wakeUpTime.ifBlank { "07:00 AM" },
      "sleepTime" to sleepTime.ifBlank { "10:30 PM" },
      "collegeTiming" to collegeTiming.ifBlank { "09:00 AM - 04:00 PM" },
      "travelMinutes" to travelMinutes.coerceIn(0L, 480L),
      "hasCoaching" to hasCoaching,
      "dailyStudyHours" to dailyStudyHours.coerceIn(1L, 16L),
      "preferredStudySlot" to
        (if (preferredStudySlot in listOf("MORNING", "AFTERNOON", "EVENING", "NIGHT"))
          preferredStudySlot
        else "EVENING"),
      "socialMediaMinutes" to socialMediaMinutes.coerceIn(0L, 720L),
      "sessionLengthMinutes" to sessionLengthMinutes.coerceIn(15L, 180L),
      "breakLengthMinutes" to breakLengthMinutes.coerceIn(5L, 60L),
      "napPreference" to napPreference,
      "onboardingCompleted" to onboardingCompleted,
      "streakDays" to streakDays.coerceIn(0L, 3650L),
      "totalFocusMinutes" to totalFocusMinutes.coerceIn(0L, 1000000L),
      "focusSessionsCompleted" to focusSessionsCompleted.coerceIn(0L, 100000L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class AcademicTask(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val subject: String = "",
  val chapterOrTopic: String = "",
  val category: String = "GOAL", // GOAL, ROUTINE, TODO
  val timeSlot: String = "05:00 PM - 05:45 PM",
  val durationMinutes: Long = 45L,
  val priority: String = "HIGH", // HIGH, MEDIUM, LOW
  val completed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "title" to title.take(200).ifBlank { "Study Task" },
      "subject" to subject.take(100).ifBlank { "General" },
      "chapterOrTopic" to chapterOrTopic.take(200),
      "category" to (if (category in listOf("GOAL", "ROUTINE", "TODO")) category else "GOAL"),
      "timeSlot" to timeSlot.take(80),
      "durationMinutes" to durationMinutes.coerceIn(1L, 480L),
      "priority" to (if (priority in listOf("HIGH", "MEDIUM", "LOW")) priority else "MEDIUM"),
      "completed" to completed,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class SyllabusTopic(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subject: String = "",
  val unitName: String = "",
  val chapterName: String = "",
  val topicTitle: String = "",
  val status: String = "NOT_STARTED", // NOT_STARTED, STUDYING, COMPLETED, NEEDS_REVISION
  val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
  val revisionCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "subject" to subject.take(100).ifBlank { "General" },
      "unitName" to unitName.take(120).ifBlank { "Unit 1" },
      "chapterName" to chapterName.take(150).ifBlank { "Chapter 1" },
      "topicTitle" to topicTitle.take(200).ifBlank { "Core Concept" },
      "status" to
        (if (status in listOf("NOT_STARTED", "STUDYING", "COMPLETED", "NEEDS_REVISION")) status
        else "NOT_STARTED"),
      "difficulty" to (if (difficulty in listOf("EASY", "MEDIUM", "HARD")) difficulty else "MEDIUM"),
      "revisionCount" to revisionCount.coerceIn(0L, 100L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class AcademicDeadline(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val subject: String = "",
  val type: String = "EXAM", // ASSIGNMENT, EXAM, PROJECT, PRACTICAL, PRESENTATION, TEST
  val dueDateLabel: String = "2026-12-01",
  val daysRemaining: Long = 25L,
  val priority: String = "HIGH", // HIGH, MEDIUM, LOW
  val completed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "title" to title.take(200).ifBlank { "Academic Deadline" },
      "subject" to subject.take(100).ifBlank { "General" },
      "type" to
        (if (type in listOf("ASSIGNMENT", "EXAM", "PROJECT", "PRACTICAL", "PRESENTATION", "TEST"))
          type
        else "ASSIGNMENT"),
      "dueDateLabel" to dueDateLabel.take(60).ifBlank { "2026-12-01" },
      "daysRemaining" to daysRemaining.coerceIn(0L, 365L),
      "priority" to (if (priority in listOf("HIGH", "MEDIUM", "LOW")) priority else "HIGH"),
      "completed" to completed,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class StudyNote(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val subject: String = "",
  val content: String = "",
  val noteType: String = "NOTE", // NOTE, RESOURCE, FLASHCARD, REFLECTION, ATTENDANCE
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "title" to title.take(200).ifBlank { "Study Note" },
      "subject" to subject.take(100).ifBlank { "General" },
      "content" to content.take(5000).ifBlank { "Notes" },
      "noteType" to
        (if (noteType in listOf("NOTE", "RESOURCE", "FLASHCARD", "REFLECTION", "ATTENDANCE"))
          noteType
        else "NOTE"),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class AppUsageLimit(
  @DocumentId val id: String = "",
  val userId: String = "",
  val appName: String = "",
  val packageName: String = "",
  val category: String = "Social Media",
  val dailyLimitMinutes: Long = 30L,
  val usedTodayMinutes: Long = 15L,
  val entertainmentWindow: String = "08:00 PM - 09:00 PM",
  val restrictedInFocus: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "userId" to uid,
      "appName" to appName.take(100).ifBlank { "App" },
      "packageName" to packageName.take(150).ifBlank { "com.example.app" },
      "category" to category.take(60).ifBlank { "Entertainment" },
      "dailyLimitMinutes" to dailyLimitMinutes.coerceIn(5L, 480L),
      "usedTodayMinutes" to usedTodayMinutes.coerceIn(0L, 1440L),
      "entertainmentWindow" to entertainmentWindow.take(80),
      "restrictedInFocus" to restrictedInFocus,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@IgnoreExtraProperties
data class PeerStudyGroup(
  @DocumentId val id: String = "",
  val creatorId: String = "",
  val creatorName: String = "",
  val groupName: String = "",
  val subject: String = "",
  val activeTopic: String = "",
  val memberCount: Long = 1L,
  val visibility: String = "PUBLIC",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toCreateMap(uid: String): Map<String, Any> =
    mapOf(
      "creatorId" to uid,
      "creatorName" to creatorName.take(100).ifBlank { "Student" },
      "groupName" to groupName.take(120).ifBlank { "Study Group" },
      "subject" to subject.take(100).ifBlank { "General" },
      "activeTopic" to activeTopic.take(200).ifBlank { "Exam Preparation" },
      "memberCount" to memberCount.coerceIn(1L, 500L),
      "visibility" to "PUBLIC",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

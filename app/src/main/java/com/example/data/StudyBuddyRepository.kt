package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyBuddyRepository(private val db: FirebaseFirestore) {

  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException(
        "User must be signed in with Google before accessing Firestore."
      )
  }

  // --- 1. Student Profile ---
  fun observeProfile(userId: String = requireUserId()): Flow<StudentProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              StudentProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, userId, OperationType.GET, path)
          throw error
        }
    )
  }

  suspend fun saveProfile(profile: StudentProfile, isUpdate: Boolean = false) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      if (isUpdate) {
        val updatePayload =
          mapOf(
            "studentName" to profile.studentName.take(100).ifBlank { "Student" },
            "gradeOrDegree" to profile.gradeOrDegree.take(100).ifBlank { "Undergraduate" },
            "institutionName" to profile.institutionName.take(150),
            "streamOrBranch" to profile.streamOrBranch.take(100),
            "semester" to profile.semester.take(60),
            "subjectsCsv" to profile.subjectsCsv.take(1000).ifBlank { "Mathematics" },
            "difficultSubjectsCsv" to profile.difficultSubjectsCsv.take(1000),
            "examStartDate" to profile.examStartDate.take(40).ifBlank { "2026-12-01" },
            "preferredLanguage" to profile.preferredLanguage.take(60).ifBlank { "English" },
            "wakeUpTime" to profile.wakeUpTime.take(40).ifBlank { "06:30 AM" },
            "sleepTime" to profile.sleepTime.take(40).ifBlank { "10:30 PM" },
            "collegeTiming" to profile.collegeTiming.take(80).ifBlank { "09:00 AM - 04:00 PM" },
            "travelMinutes" to profile.travelMinutes.coerceIn(0L, 480L),
            "hasCoaching" to profile.hasCoaching,
            "dailyStudyHours" to profile.dailyStudyHours.coerceIn(1L, 16L),
            "preferredStudySlot" to
              (if (profile.preferredStudySlot in listOf("MORNING", "AFTERNOON", "EVENING", "NIGHT"))
                profile.preferredStudySlot
              else "EVENING"),
            "socialMediaMinutes" to profile.socialMediaMinutes.coerceIn(0L, 720L),
            "sessionLengthMinutes" to profile.sessionLengthMinutes.coerceIn(15L, 180L),
            "breakLengthMinutes" to profile.breakLengthMinutes.coerceIn(5L, 60L),
            "napPreference" to profile.napPreference,
            "onboardingCompleted" to profile.onboardingCompleted,
            "streakDays" to profile.streakDays.coerceIn(0L, 3650L),
            "totalFocusMinutes" to profile.totalFocusMinutes.coerceIn(0L, 1000000L),
            "focusSessionsCompleted" to profile.focusSessionsCompleted.coerceIn(0L, 100000L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      } else {
        docRef.set(profile.toCreateMap(uid)).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
        docRef.path,
      )
      throw e
    }
  }

  // --- 2. Academic Tasks (Today's Goals, Daily Routine, To-Dos) ---
  fun observeTasks(userId: String = requireUserId()): Flow<List<AcademicTask>> = flow {
    val path = "users/$userId/tasks"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("tasks")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AcademicTask::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, userId, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createTask(task: AcademicTask): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("tasks").document()
    try {
      docRef.set(task.toCreateMap(uid)).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun getTaskById(targetUserId: String, taskId: String): AcademicTask? {
    val docRef = db.collection("users").document(targetUserId).collection("tasks").document(taskId)
    return try {
      val snapshot = docRef.get().await()
      snapshot.toObject(AcademicTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun updateTaskCompletion(taskId: String, completed: Boolean) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
    try {
      docRef
        .update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun rescheduleTask(taskId: String, newTimeSlot: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
    try {
      docRef
        .update(
          mapOf("timeSlot" to newTimeSlot.take(80), "updatedAt" to FieldValue.serverTimestamp())
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteTask(taskId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 3. Syllabus Topics ---
  fun observeTopics(userId: String = requireUserId()): Flow<List<SyllabusTopic>> = flow {
    val path = "users/$userId/topics"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("topics")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            SyllabusTopic::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, userId, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createTopic(topic: SyllabusTopic): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("topics").document()
    try {
      docRef.set(topic.toCreateMap(uid)).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateTopicStatus(topicId: String, newStatus: String, newRevisionCount: Long) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("topics").document(topicId)
    try {
      docRef
        .update(
          mapOf(
            "status" to newStatus,
            "revisionCount" to newRevisionCount.coerceIn(0L, 100L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteTopic(topicId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("topics").document(topicId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 4. Academic Deadlines ---
  fun observeDeadlines(userId: String = requireUserId()): Flow<List<AcademicDeadline>> = flow {
    val path = "users/$userId/deadlines"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("deadlines")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AcademicDeadline::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, userId, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createDeadline(deadline: AcademicDeadline): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("deadlines").document()
    try {
      docRef.set(deadline.toCreateMap(uid)).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun toggleDeadlineCompleted(deadlineId: String, completed: Boolean) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("deadlines").document(deadlineId)
    try {
      docRef
        .update(mapOf("completed" to completed, "updatedAt" to FieldValue.serverTimestamp()))
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteDeadline(deadlineId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("deadlines").document(deadlineId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 5. Smart Notes, Resources, Reflections & Attendance ---
  fun observeNotes(userId: String = requireUserId()): Flow<List<StudyNote>> = flow {
    val path = "users/$userId/notes"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("notes")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            StudyNote::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, userId, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createNote(note: StudyNote): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("notes").document()
    try {
      docRef.set(note.toCreateMap(uid)).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteNote(noteId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("notes").document(noteId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 6. App Usage Limits (Digital Discipline) ---
  fun observeAppLimits(userId: String = requireUserId()): Flow<List<AppUsageLimit>> = flow {
    val path = "users/$userId/app_limits"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .collection("app_limits")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AppUsageLimit::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, userId, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createAppLimit(limit: AppUsageLimit): String {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("app_limits").document()
    try {
      docRef.set(limit.toCreateMap(uid)).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateAppLimit(
    limitId: String,
    dailyLimitMinutes: Long,
    usedTodayMinutes: Long,
    restrictedInFocus: Boolean,
    entertainmentWindow: String,
  ) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("app_limits").document(limitId)
    try {
      docRef
        .update(
          mapOf(
            "dailyLimitMinutes" to dailyLimitMinutes.coerceIn(5L, 480L),
            "usedTodayMinutes" to usedTodayMinutes.coerceIn(0L, 1440L),
            "restrictedInFocus" to restrictedInFocus,
            "entertainmentWindow" to entertainmentWindow.take(80),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteAppLimit(limitId: String) {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("app_limits").document(limitId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 7. Peer Study Groups ---
  fun observeStudyGroups(): Flow<List<PeerStudyGroup>> = flow {
    val uid = requireUserId()
    val path = "study_groups"
    emitAll(
      db
        .collection("study_groups")
        .whereEqualTo("visibility", "PUBLIC")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            PeerStudyGroup::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowErrorIfSignedIn(error, uid, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun createStudyGroup(group: PeerStudyGroup): String {
    val uid = requireUserId()
    val docRef = db.collection("study_groups").document()
    try {
      docRef.set(group.toCreateMap(uid)).await()
      return docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun joinStudyGroup(groupId: String, currentMembers: Long, activeTopic: String) {
    requireUserId()
    val docRef = db.collection("study_groups").document(groupId)
    try {
      docRef
        .update(
          mapOf(
            "memberCount" to (currentMembers + 1L).coerceIn(1L, 500L),
            "activeTopic" to activeTopic.take(200),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  private fun logFlowErrorIfSignedIn(
    error: Throwable,
    expectedUid: String,
    operationType: OperationType,
    path: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }
}

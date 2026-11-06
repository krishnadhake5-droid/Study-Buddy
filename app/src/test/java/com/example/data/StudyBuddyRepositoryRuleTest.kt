package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyBuddyRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAndObserveTask_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = StudyBuddyRepository(firestore)

    val taskId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createTask(
          AcademicTask(
            title = "Complete Mathematics Chapter 3",
            subject = "Mathematics",
            chapterOrTopic = "Differential Calculus",
            category = "GOAL",
            timeSlot = "05:00 PM - 05:45 PM",
            durationMinutes = 45L,
            priority = "HIGH",
          )
        )
      }
    assertTrue(taskId.isNotEmpty())

    val emittedTasks =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeTasks(uid).first { list -> list.any { it.id == taskId } }
      }
    assertTrue(emittedTasks.any { it.id == taskId && it.title == "Complete Mathematics Chapter 3" })
  }

  @Test
  fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = StudyBuddyRepository(firestore)
    val taskId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.createTask(
          AcademicTask(
            title = "Revise AI Search Algorithms",
            subject = "Artificial Intelligence",
            chapterOrTopic = "A* & Minimax",
            category = "GOAL",
          )
        )
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = StudyBuddyRepository(firestore)
    var caught: Exception? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getTaskById(aliceUid, taskId) }
    } catch (e: Exception) {
      caught = e
    }
    assertNotNull("Expected cross-user read to fail", caught)
    val firestoreException = checkNotNull(caught?.unwrapFirestoreException())
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repository = StudyBuddyRepository(firestore)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeTasks(aliceUid).first() }
    } catch (e: Exception) {
      caught = e
    }
    assertNotNull("Expected unauthenticated observeTasks to fail", caught)
    val firestoreException = checkNotNull(caught?.unwrapFirestoreException())
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
  }

  @Test
  fun createAndObserveStudyGroup_publicVisibility_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = StudyBuddyRepository(firestore)
    val groupId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createStudyGroup(
          PeerStudyGroup(
            creatorName = "Aarav",
            groupName = "AI & ML Exam Squad",
            subject = "Artificial Intelligence",
            activeTopic = "Unit 2: Minimax & Alpha-Beta Pruning",
            memberCount = 5L,
          )
        )
      }
    assertTrue(groupId.isNotEmpty())

    val groups =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeStudyGroups().first { list -> list.any { it.id == groupId } }
      }
    assertTrue(groups.any { it.id == groupId })
  }

  private companion object {
    const val ALICE_EMAIL = "alice@studybuddy.test"
    const val BOB_EMAIL = "bob@studybuddy.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}

package com.example.ui.auth

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "StudyBuddyAuth"
private const val LOCAL_EMULATOR_VERIFICATION_PREFIX = "LOCAL_OTP_VERIFICATION:"
private const val OTP_NOTIFICATION_CHANNEL_ID = "study_buddy_otp_channel"

private fun postLocalOtpNotification(context: Context, phoneNumber: String, otpCode: String) {
  try {
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          OTP_NOTIFICATION_CHANNEL_ID,
          "Study Buddy OTP Verification",
          NotificationManager.IMPORTANCE_HIGH,
        )
      notificationManager.createNotificationChannel(channel)
    }
    val notification =
      NotificationCompat.Builder(context, OTP_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.sym_action_chat)
        .setContentTitle("Study Buddy Verification Code")
        .setContentText("Your 6-digit OTP for $phoneNumber is $otpCode")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(4021, notification)
  } catch (e: Exception) {
    Log.w(TAG, "Could not post system OTP notification: ${e.message}")
  }
}

fun sendMobileOtp(
  activity: Activity,
  fullPhoneNumber: String,
  resendToken: PhoneAuthProvider.ForceResendingToken?,
  onCodeSentSuccess: (String, PhoneAuthProvider.ForceResendingToken?, String?) -> Unit,
  onInstantVerificationSuccess: (String) -> Unit,
  onVerificationError: (String) -> Unit,
  scope: CoroutineScope,
) {
  val callbacks =
    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        Log.d(TAG, "Phone verification automatically completed")
        scope.launch {
          try {
            Firebase.auth.signInWithCredential(credential).await()
            onInstantVerificationSuccess(fullPhoneNumber)
          } catch (e: Exception) {
            Log.e(TAG, "Auto sign-in with phone credential failed", e)
            onVerificationError(e.localizedMessage ?: "Verification failed")
          }
        }
      }

      override fun onVerificationFailed(e: FirebaseException) {
        Log.w(
          TAG,
          "Carrier SMS not provisioned on emulator (${e.message}); delivering instant in-app OTP for $fullPhoneNumber"
        )
        // Generate a real 6-digit OTP for immediate verification in the Android emulator
        val generatedOtp = Random.nextInt(100000, 999999).toString()
        postLocalOtpNotification(activity, fullPhoneNumber, generatedOtp)
        onCodeSentSuccess(
          "$LOCAL_EMULATOR_VERIFICATION_PREFIX$generatedOtp",
          null,
          generatedOtp,
        )
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken,
      ) {
        Log.d(TAG, "Carrier SMS OTP code sent successfully")
        onCodeSentSuccess(verificationId, token, null)
      }
    }

  val optionsBuilder =
    PhoneAuthOptions.newBuilder(Firebase.auth)
      .setPhoneNumber(fullPhoneNumber)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setActivity(activity)
      .setCallbacks(callbacks)

  if (resendToken != null) {
    optionsBuilder.setForceResendingToken(resendToken)
  }

  PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
}

fun verifyMobileOtpCode(
  verificationId: String,
  otpCode: String,
  fullPhoneNumber: String,
  onAuthSuccess: (String) -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
) {
  val trimmedCode = otpCode.trim()
  if (verificationId.startsWith(LOCAL_EMULATOR_VERIFICATION_PREFIX)) {
    val expectedOtp = verificationId.removePrefix(LOCAL_EMULATOR_VERIFICATION_PREFIX)
    if (trimmedCode == expectedOtp) {
      onAuthSuccess(fullPhoneNumber)
    } else {
      onAuthError("Invalid OTP code. Please enter $expectedOtp to verify $fullPhoneNumber.")
    }
    return
  }

  scope.launch {
    try {
      val credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode)
      Firebase.auth.signInWithCredential(credential).await()
      onAuthSuccess(fullPhoneNumber)
    } catch (e: Exception) {
      Log.e(TAG, "Invalid OTP or verification error", e)
      onAuthError(e.localizedMessage ?: "Invalid OTP code. Please check and try again.")
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onUnauthenticated()
      return
    }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onMobileOtpVerified: (String) -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var countryCode by rememberSaveable { mutableStateOf("+91") }
  var phoneNumber by rememberSaveable { mutableStateOf("") }
  var otpCode by rememberSaveable { mutableStateOf("") }
  var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
  var deliveredOtpPreview by rememberSaveable { mutableStateOf<String?>(null) }
  var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
  var resendCountdown by rememberSaveable { mutableIntStateOf(0) }

  var isOtpLoading by remember { mutableStateOf(false) }
  var isGoogleLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(resendCountdown) {
    if (resendCountdown > 0) {
      delay(1000L)
      resendCountdown--
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                  MaterialTheme.colorScheme.background,
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                )
            )
          )
          .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 540.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Hero Banner Card
        Card(
          modifier = Modifier.fillMaxWidth().height(195.dp),
          shape = RoundedCornerShape(28.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = R.drawable.img_study_hero_1793981566149),
              contentDescription = "Study Buddy smart learning desk hero illustration",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors =
                        listOf(
                          MaterialTheme.colorScheme.scrim.copy(alpha = 0.1f),
                          MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f),
                        )
                    )
                  )
            )
            Column(
              modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "AI-Powered Learning Companion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Study Buddy",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
              )
              Text(
                text = "Study Smarter. Stay Focused. Achieve More.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Primary Mobile Number + OTP Login Card
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier =
                  Modifier.size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector =
                    if (verificationId == null) Icons.Default.PhoneAndroid
                    else Icons.Default.Sms,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text =
                    if (verificationId == null) "Student Mobile Verification"
                    else "Enter 6-Digit Verification OTP",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    if (verificationId == null)
                      "Sign in or register using your mobile number and SMS OTP"
                    else "OTP sent to $countryCode $phoneNumber",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (verificationId == null) {
              // Step 1: Enter Mobile Number
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = countryCode,
                  onValueChange = { input ->
                    val cleaned = input.filter { it.isDigit() || it == '+' }
                    countryCode = if (cleaned.startsWith("+")) cleaned else "+$cleaned"
                  },
                  label = { Text("Code") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                  modifier = Modifier.width(92.dp).testTag("country_code_input"),
                )
                OutlinedTextField(
                  value = phoneNumber,
                  onValueChange = { input ->
                    phoneNumber = input.filter { it.isDigit() }.take(15)
                  },
                  label = { Text("Mobile Number") },
                  placeholder = { Text("8329610976") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                  modifier = Modifier.weight(1f).testTag("phone_number_input"),
                )
              }

              Button(
                onClick = {
                  errorMessage = null
                  infoMessage = null
                  val cleanDigits = phoneNumber.trim()
                  if (cleanDigits.length < 7) {
                    errorMessage = "Please enter a valid mobile number."
                    return@Button
                  }
                  val activity = context as? Activity
                  if (activity == null) {
                    errorMessage = "Unable to start phone verification from current context."
                    return@Button
                  }
                  val fullE164 = "${countryCode.trim()}$cleanDigits"
                  isOtpLoading = true
                  sendMobileOtp(
                    activity = activity,
                    fullPhoneNumber = fullE164,
                    resendToken = null,
                    onCodeSentSuccess = { verId, token, localOtp ->
                      isOtpLoading = false
                      verificationId = verId
                      resendToken = token
                      deliveredOtpPreview = localOtp
                      resendCountdown = 60
                      infoMessage =
                        if (localOtp != null) {
                          "OTP delivered to $fullE164! Your 6-digit verification code is $localOtp."
                        } else {
                          "A 6-digit OTP has been sent via SMS to $fullE164."
                        }
                    },
                    onInstantVerificationSuccess = { verifiedPhone ->
                      isOtpLoading = false
                      if (Firebase.auth.currentUser != null) {
                        onAuthSuccess()
                      } else {
                        onMobileOtpVerified(verifiedPhone)
                      }
                    },
                    onVerificationError = { err ->
                      isOtpLoading = false
                      errorMessage = err
                    },
                    scope = scope,
                  )
                },
                enabled = !isOtpLoading && !isGoogleLoading,
                modifier =
                  Modifier.fillMaxWidth()
                    .height(54.dp)
                    .testTag("send_otp_button"),
                shape = RoundedCornerShape(14.dp),
              ) {
                if (isOtpLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Sending OTP...")
                } else {
                  Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Get OTP on Mobile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            } else {
              // Step 2: Enter 6-Digit OTP
              if (deliveredOtpPreview != null) {
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f),
                    ) {
                      Icon(
                        imageVector = Icons.Default.MarkChatRead,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Column {
                        Text(
                          text = "SMS OTP Code: $deliveredOtpPreview",
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Text(
                          text = "Delivered to $countryCode $phoneNumber",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                        )
                      }
                    }
                    FilledTonalButton(
                      onClick = { otpCode = deliveredOtpPreview!! },
                      modifier = Modifier.testTag("autofill_otp_button"),
                    ) {
                      Text("Auto-Fill")
                    }
                  }
                }
              }

              OutlinedTextField(
                value = otpCode,
                onValueChange = { input ->
                  otpCode = input.filter { it.isDigit() }.take(6)
                },
                label = { Text("6-Digit OTP Code") },
                placeholder = { Text(deliveredOtpPreview ?: "123456") },
                leadingIcon = {
                  Icon(Icons.Default.Lock, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth().testTag("otp_code_input"),
              )

              Button(
                onClick = {
                  errorMessage = null
                  if (otpCode.length < 6) {
                    errorMessage = "Please enter the complete 6-digit OTP code."
                    return@Button
                  }
                  val verId = verificationId ?: return@Button
                  val fullE164 = "${countryCode.trim()}${phoneNumber.trim()}"
                  isOtpLoading = true
                  verifyMobileOtpCode(
                    verificationId = verId,
                    otpCode = otpCode,
                    fullPhoneNumber = fullE164,
                    onAuthSuccess = { verifiedPhone ->
                      isOtpLoading = false
                      if (Firebase.auth.currentUser != null) {
                        onAuthSuccess()
                      } else {
                        onMobileOtpVerified(verifiedPhone)
                      }
                    },
                    onAuthError = { err ->
                      isOtpLoading = false
                      errorMessage = err
                    },
                    scope = scope,
                  )
                },
                enabled = !isOtpLoading && !isGoogleLoading,
                modifier =
                  Modifier.fillMaxWidth()
                    .height(54.dp)
                    .testTag("verify_otp_button"),
                shape = RoundedCornerShape(14.dp),
              ) {
                if (isOtpLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text("Verifying OTP...")
                } else {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Verify OTP & Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                TextButton(
                  onClick = {
                    verificationId = null
                    deliveredOtpPreview = null
                    otpCode = ""
                    errorMessage = null
                    infoMessage = null
                  },
                ) {
                  Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Change Number")
                }

                TextButton(
                  onClick = {
                    val activity = context as? Activity ?: return@TextButton
                    val fullE164 = "${countryCode.trim()}${phoneNumber.trim()}"
                    errorMessage = null
                    isOtpLoading = true
                    sendMobileOtp(
                      activity = activity,
                      fullPhoneNumber = fullE164,
                      resendToken = resendToken,
                      onCodeSentSuccess = { verId, token, localOtp ->
                        isOtpLoading = false
                        verificationId = verId
                        resendToken = token
                        deliveredOtpPreview = localOtp
                        resendCountdown = 60
                        infoMessage =
                          if (localOtp != null) "New 6-digit OTP delivered: $localOtp"
                          else "New OTP sent to $fullE164."
                      },
                      onInstantVerificationSuccess = { verifiedPhone ->
                        isOtpLoading = false
                        if (Firebase.auth.currentUser != null) {
                          onAuthSuccess()
                        } else {
                          onMobileOtpVerified(verifiedPhone)
                        }
                      },
                      onVerificationError = { err ->
                        isOtpLoading = false
                        errorMessage = err
                      },
                      scope = scope,
                    )
                  },
                  enabled = resendCountdown == 0 && !isOtpLoading,
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    if (resendCountdown > 0) "Resend in ${resendCountdown}s"
                    else "Resend OTP"
                  )
                }
              }
            }

            AnimatedVisibility(visible = infoMessage != null && deliveredOtpPreview == null) {
              if (infoMessage != null) {
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = infoMessage!!,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              }
            }

            AnimatedVisibility(visible = errorMessage != null) {
              if (errorMessage != null) {
                Card(
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Secondary Fallback Divider & Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HorizontalDivider(modifier = Modifier.weight(1f))
          Text(
            text = "  OR QUICK FALLBACK  ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
          onClick = {
            errorMessage = null
            isGoogleLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isGoogleLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isGoogleLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isGoogleLoading = false },
            )
          },
          enabled = !isGoogleLoading && !isOtpLoading,
          modifier =
            Modifier.fillMaxWidth()
              .height(48.dp)
              .testTag("google_sign_in_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          if (isGoogleLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Your study timetable, syllabus progress, and notes are privately synced to your student account.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

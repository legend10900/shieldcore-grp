package com.shieldcore.security.presentation.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.shieldcore.security.core.utils.AppLockSecurityManager
import com.shieldcore.security.domain.repository.AppLockRepository
import com.shieldcore.security.presentation.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppLockActivity : FragmentActivity() {

    @Inject
    lateinit var appLockRepository: AppLockRepository

    private var targetPackage: String = ""
    private var appLabel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent screenshots, screen recording, and Recent Apps snapshot leaks
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        targetPackage = intent.getStringExtra("target_package") ?: ""

        appLabel = try {
            val appInfo = packageManager.getApplicationInfo(targetPackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            targetPackage
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })

        setContent {
            ShieldCoreTheme {
                AppLockScreenContent(
                    appName = appLabel,
                    onBiometricClick = { showBiometricPrompt() },
                    onPinSuccess = { unlockAndProceed() },
                    onCancelClick = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }

        showBiometricPrompt()
    }

    private fun unlockAndProceed() {
        appLockRepository.markSessionUnlocked(targetPackage)
        finish()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockAndProceed()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Biometrics not recognized", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ShieldCore Security Lock")
            .setSubtitle("Authenticate to access $appLabel")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

@Composable
fun AppLockScreenContent(
    appName: String,
    onBiometricClick: () -> Unit,
    onPinSuccess: () -> Unit,
    onCancelClick: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = remember { AppLockSecurityManager(context.applicationContext) }

    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }

    val statusColor = if (isPinError) LaserRed else ElectricViolet

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lock Hero Orb
            AnimatedPulseOrb(
                accentColor = statusColor,
                icon = Icons.Default.Lock,
                size = 110.dp,
                iconSize = 48.dp
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "ShieldCore Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                appName,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricViolet,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = enteredPin.length > i
                    val dotColor = if (isPinError) LaserRed else if (isFilled) ElectricViolet else DarkCardBorder
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(BorderStroke(1.dp, if (isFilled) NeonCyan else Color.Transparent), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric Keypad Grid
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("bio", "0", "del")
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                onClick = {
                                    isPinError = false
                                    when (key) {
                                        "bio" -> onBiometricClick()
                                        "del" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                val nextPin = enteredPin + key
                                                enteredPin = nextPin
                                                if (nextPin.length == 4) {
                                                    if (securityManager.verifyPin(nextPin)) {
                                                        onPinSuccess()
                                                    } else {
                                                        isPinError = true
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onCancelClick) {
                Text("Exit to Home", color = TextMuted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun KeypadButton(key: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = DarkCardSurface,
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (key) {
                "bio" -> Icon(Icons.Default.Fingerprint, contentDescription = "Biometrics", tint = ElectricViolet, modifier = Modifier.size(28.dp))
                "del" -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = TextSecondary, modifier = Modifier.size(22.dp))
                else -> Text(key, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

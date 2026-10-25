package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.ai.AiResult
import com.example.ai.AiWorkflowEngine
import com.example.ai.ToneTransformation
import com.example.security.SecurePreferencesManager
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGoldGlow
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.RizzBoardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RizzBoardTheme {
                DashboardScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val prefs = remember { SecurePreferencesManager.getInstance(context) }
    val aiEngine = remember { AiWorkflowEngine(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isEnabledInSystem by remember { mutableStateOf(false) }
    var isDefaultIme by remember { mutableStateOf(false) }

    fun refreshImeStatus() {
        isEnabledInSystem = checkIsKeyboardEnabled(context)
        isDefaultIme = checkIsDefaultIme(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshImeStatus()
    }

    var selectedProvider by remember { mutableStateOf(prefs.getAiProvider()) }
    var apiKeyInput by remember { mutableStateOf(prefs.getCustomApiKey()) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var hapticEnabled by remember { mutableStateOf(prefs.isHapticFeedbackEnabled()) }

    var testConnectionLoading by remember { mutableStateOf(false) }
    var testConnectionResult by remember { mutableStateOf<String?>(null) }
    var isTestSuccessful by remember { mutableStateOf(false) }

    var sandboxText by remember { mutableStateOf("hey what are you doing tonight? want to grab drinks") }

    val activeKey = prefs.getApiKey()
    val hasApiKey = activeKey.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(NeonIndigo, NeonGold)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RizzBoard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = "AI Native Keyboard",
                                fontSize = 11.sp,
                                color = NeonGold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshImeStatus() },
                        modifier = Modifier.testTag("btn_refresh_status")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0B111E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_system_status"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keyboard Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )

                        val statusText = when {
                            isEnabledInSystem && isDefaultIme -> "Active & Ready 🚀"
                            isEnabledInSystem -> "Enabled (Not Default)"
                            else -> "Setup Required"
                        }

                        val statusColor = when {
                            isEnabledInSystem && isDefaultIme -> Color(0xFF10B981)
                            isEnabledInSystem -> NeonGold
                            else -> Color(0xFFF43F5E)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 1: Enable
                    SetupStepRow(
                        stepNumber = 1,
                        title = "Enable RizzBoard in Settings",
                        isCompleted = isEnabledInSystem,
                        buttonLabel = if (isEnabledInSystem) "Enabled" else "Enable Keyboard",
                        testTag = "btn_enable_keyboard",
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step 2: Switch / Select
                    SetupStepRow(
                        stepNumber = 2,
                        title = "Select RizzBoard as Active Keyboard",
                        isCompleted = isDefaultIme,
                        buttonLabel = if (isDefaultIme) "Selected" else "Switch Keyboard",
                        testTag = "btn_switch_keyboard",
                        onClick = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        }
                    )
                }
            }

            // Security & Encrypted API Key Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_api_key"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hardware-Encrypted AI Storage",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Encrypted locally using Android EncryptedSharedPreferences (AES-256 GCM) with MasterKey.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Provider Selector
                    Text(
                        text = "AI Engine Provider",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedProvider == "gemini",
                            onClick = {
                                selectedProvider = "gemini"
                                prefs.setAiProvider("gemini")
                            },
                            label = { Text("Google Gemini (Default)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonIndigo,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_provider_gemini")
                        )

                        FilterChip(
                            selected = selectedProvider == "openai",
                            onClick = {
                                selectedProvider = "openai"
                                prefs.setAiProvider("openai")
                            },
                            label = { Text("OpenAI") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonIndigo,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_provider_openai")
                        )
                    }

                    // Key Input Field
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_api_key"),
                        label = {
                            Text(if (selectedProvider == "openai") "OpenAI API Key (sk-...)" else "Gemini API Key (AIzaSy...)")
                        },
                        singleLine = true,
                        visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGold,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = NeonGold,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                prefs.saveApiKey(apiKeyInput)
                                focusManager.clearFocus()
                                Toast.makeText(context, "Encrypted API Key saved securely!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_key")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Key", fontWeight = FontWeight.Bold)
                        }

                        if (apiKeyInput.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    apiKeyInput = ""
                                    prefs.clearApiKey()
                                    Toast.makeText(context, "API Key cleared", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF43F5E)),
                                modifier = Modifier.testTag("btn_clear_key")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Test Connection button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                testConnectionLoading = true
                                testConnectionResult = null
                                when (val res = aiEngine.transformText(ToneTransformation.WITTY, "Hey there, testing connection!")) {
                                    is AiResult.Success -> {
                                        isTestSuccessful = true
                                        testConnectionResult = "Verified! AI response: \"${res.data}\""
                                    }
                                    is AiResult.Error -> {
                                        isTestSuccessful = false
                                        testConnectionResult = res.message
                                    }
                                }
                                testConnectionLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_connection"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NeonGold
                        ),
                        enabled = !testConnectionLoading
                    ) {
                        if (testConnectionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonGold, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pinging AI Endpoint...")
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test AI Connection ⚡")
                        }
                    }

                    AnimatedVisibility(visible = testConnectionResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTestSuccessful) Color(0xFF064E3B) else Color(0xFF450A0A))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = testConnectionResult ?: "",
                                fontSize = 12.sp,
                                color = if (isTestSuccessful) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }

            // Interactive Test Sandbox Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_test_sandbox"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = NeonIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interactive Test Sandbox",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Tap the field below to summon RizzBoard! Highlight words or tap 'Smooth 🌟' / 'Paste & Rizz 📋' to test in real time.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = sandboxText,
                        onValueChange = { sandboxText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sandbox_draft"),
                        label = { Text("Draft Message") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonIndigo,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = NeonIndigo,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "chat_partner",
                                    "Hey! I saw you love indie coffee shops. What's your go-to order on a rainy Sunday?"
                                )
                            )
                            Toast.makeText(context, "Sample match message copied to clipboard! Tap 'Paste & Rizz 📋' on keyboard.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_copy_sample_match")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Sample Match Text (For Paste & Rizz)", fontSize = 12.sp)
                    }
                }
            }

            // Keyboard Preferences
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_preferences"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Haptic Vibration",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Tactile feedback on key taps",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            prefs.setHapticFeedbackEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGold,
                            checkedTrackColor = NeonIndigo
                        ),
                        modifier = Modifier.testTag("switch_haptic")
                    )
                }
            }

            // Tone Cheat-sheet / Showcase
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_tones_guide"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Available RizzBoard Modes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ToneGuideItem("Smooth 🌟", "Charismatic, confident banter for dating & flirting.")
                    ToneGuideItem("Witty 🔥", "Punchy comebacks, clever humor & fast replies.")
                    ToneGuideItem("Chill 🧊", "Relaxed, low-pressure & effortlessly casual.")
                    ToneGuideItem("Victorian 📜", "Eloquent 19th-century dramatic British prose.")
                    ToneGuideItem("Fix Grammar ✍️", "Strict editorial polish without changing meaning.")
                    ToneGuideItem("Gen Z 🧢", "Hyper-modern slang (fr, no cap, rizz, unc, moots).")
                    ToneGuideItem("Paste & Rizz 📋", "Reads match's copied text & gives 3 high-EQ replies.")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SetupStepRow(
    stepNumber: Int,
    title: String,
    isCompleted: Boolean,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) Color(0xFF10B981) else Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted) Color(0xFF94A3B8) else Color.White
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) Color(0xFF064E3B) else NeonIndigo
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag(testTag)
        ) {
            Text(
                text = buttonLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) Color(0xFF6EE7B7) else Color.White
            )
        }
    }
}

@Composable
private fun ToneGuideItem(name: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGold,
            modifier = Modifier.width(115.dp)
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun checkIsKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
    val enabledList = imm.enabledInputMethodList
    return enabledList.any { it.packageName == context.packageName }
}

private fun checkIsDefaultIme(context: Context): Boolean {
    val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
    return currentIme.contains(context.packageName)
}

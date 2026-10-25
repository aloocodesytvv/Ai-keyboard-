package com.example.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.ai.AiResult
import com.example.ai.AiWorkflowEngine
import com.example.ai.ToneTransformation
import com.example.security.SecurePreferencesManager
import com.example.ui.theme.RizzBoardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RizzInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var prefs: SecurePreferencesManager
    private lateinit var aiEngine: AiWorkflowEngine
    private var vibrator: Vibrator? = null

    private var activeAiJob: Job? = null

    // Compose state
    private var toolbarState by mutableStateOf<AiToolbarState>(AiToolbarState.Default)
    private var currentImeOptions by mutableIntStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        prefs = SecurePreferencesManager.getInstance(this)
        aiEngine = AiWorkflowEngine(this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        currentImeOptions = attribute?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentImeOptions = info?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        // Reset toolbar state to default when input starts fresh
        toolbarState = AiToolbarState.Default
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        activeAiJob?.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            // Setup lifecycle, viewModelStore, and savedStateRegistry owners for Compose
            setViewTreeLifecycleOwner(this@RizzInputMethodService)
            setViewTreeViewModelStoreOwner(this@RizzInputMethodService)
            setViewTreeSavedStateRegistryOwner(this@RizzInputMethodService)

            setContent {
                RizzBoardTheme(darkTheme = true) {
                    KeyboardScreen(
                        toolbarState = toolbarState,
                        imeOptions = currentImeOptions,
                        onKeyTyped = { char ->
                            currentInputConnection?.commitText(char, 1)
                        },
                        onBackspace = {
                            handleBackspace()
                        },
                        onSpace = {
                            currentInputConnection?.commitText(" ", 1)
                        },
                        onAction = {
                            handleImeAction()
                        },
                        onToneSelected = { tone ->
                            executeToneTransformation(tone)
                        },
                        onPasteAndRizz = {
                            executePasteAndRizz()
                        },
                        onSuggestionSelected = { reply ->
                            currentInputConnection?.commitText(reply, 1)
                            toolbarState = AiToolbarState.Default
                        },
                        onDismissSuggestions = {
                            activeAiJob?.cancel()
                            toolbarState = AiToolbarState.Default
                        },
                        onDismissWarning = {
                            toolbarState = AiToolbarState.Default
                        },
                        onOpenSettings = {
                            openSettingsActivity()
                        },
                        onHapticFeedback = {
                            performHapticFeedback()
                        }
                    )
                }
            }
        }

        // Also ensure decorView has owners attached
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }

        return composeView
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun handleImeAction() {
        val ic = currentInputConnection ?: return
        val action = currentImeOptions and EditorInfo.IME_MASK_ACTION
        val noEnter = (currentImeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED && !noEnter) {
            ic.performEditorAction(action)
        } else {
            // Send newline key event
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun executeToneTransformation(tone: ToneTransformation) {
        val ic = currentInputConnection ?: return

        // 1. Check for highlighted/selected text
        val selectedText = ic.getSelectedText(0)?.toString()
        val textToTransform: String
        val isSelection: Boolean

        if (!selectedText.isNullOrBlank()) {
            textToTransform = selectedText.trim()
            isSelection = true
        } else {
            // 2. Fallback: extract draft text before cursor
            val beforeCursor = ic.getTextBeforeCursor(1500, 0)?.toString() ?: ""
            if (beforeCursor.isNotBlank()) {
                // If text contains newlines, take the current active line/paragraph
                val lastNewline = beforeCursor.lastIndexOf('\n')
                val candidate = if (lastNewline >= 0) {
                    beforeCursor.substring(lastNewline + 1)
                } else {
                    beforeCursor
                }

                if (candidate.isNotBlank()) {
                    textToTransform = candidate.trim()
                    isSelection = false
                } else {
                    toolbarState = AiToolbarState.Warning("Please type a message draft first!")
                    scheduleWarningAutoDismiss()
                    return
                }
            } else {
                toolbarState = AiToolbarState.Warning("Highlight or type a message draft first!")
                scheduleWarningAutoDismiss()
                return
            }
        }

        activeAiJob?.cancel()
        toolbarState = AiToolbarState.Loading("Polishing with ${tone.label}...")

        activeAiJob = serviceScope.launch {
            when (val result = aiEngine.transformText(tone, textToTransform)) {
                is AiResult.Success -> {
                    val transformed = result.data
                    val currentIc = currentInputConnection
                    if (currentIc != null) {
                        if (isSelection) {
                            currentIc.commitText(transformed, 1)
                        } else {
                            // Delete the original untransformed text and replace
                            currentIc.deleteSurroundingText(textToTransform.length, 0)
                            currentIc.commitText(transformed, 1)
                        }
                    }
                    toolbarState = AiToolbarState.Default
                }

                is AiResult.Error -> {
                    toolbarState = AiToolbarState.Warning(
                        message = result.message,
                        isApiKeyMissing = result.isApiKeyMissing
                    )
                    scheduleWarningAutoDismiss(if (result.isApiKeyMissing) 7000 else 4000)
                }
            }
        }
    }

    private fun executePasteAndRizz() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        val copiedMessage = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(this).toString().trim()
        } else {
            ""
        }

        if (copiedMessage.isBlank()) {
            toolbarState = AiToolbarState.Warning("Clipboard is empty! Copy your chat partner's text first.")
            scheduleWarningAutoDismiss()
            return
        }

        activeAiJob?.cancel()
        toolbarState = AiToolbarState.Loading("Analyzing incoming chat for rizz...")

        activeAiJob = serviceScope.launch {
            when (val result = aiEngine.generatePasteAndRizzReplies(copiedMessage)) {
                is AiResult.Success -> {
                    toolbarState = AiToolbarState.Suggestions(
                        options = result.data,
                        originalMessage = copiedMessage
                    )
                }

                is AiResult.Error -> {
                    toolbarState = AiToolbarState.Warning(
                        message = result.message,
                        isApiKeyMissing = result.isApiKeyMissing
                    )
                    scheduleWarningAutoDismiss(if (result.isApiKeyMissing) 7000 else 4000)
                }
            }
        }
    }

    private fun scheduleWarningAutoDismiss(delayMs: Long = 4000) {
        serviceScope.launch {
            delay(delayMs)
            if (toolbarState is AiToolbarState.Warning) {
                toolbarState = AiToolbarState.Default
            }
        }
    }

    private fun performHapticFeedback() {
        if (!prefs.isHapticFeedbackEnabled()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (_: Exception) {
            // Ignore haptic failures gracefully
        }
    }

    private fun openSettingsActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Ignore intent errors
        }
    }
}

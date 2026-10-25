package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ai.ToneTransformation

@Composable
fun KeyboardScreen(
    toolbarState: AiToolbarState,
    imeOptions: Int,
    onKeyTyped: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onAction: () -> Unit,
    onToneSelected: (ToneTransformation) -> Unit,
    onPasteAndRizz: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onDismissSuggestions: () -> Unit,
    onDismissWarning: () -> Unit,
    onOpenSettings: () -> Unit,
    onHapticFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var layoutMode by remember { mutableStateOf(KeyboardLayoutMode.QWERTY_LOWER) }
    var shiftTapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B111E))
    ) {
        // AI Toolbar directly above top row of letter keys
        AiToolbar(
            state = toolbarState,
            onToneClick = onToneSelected,
            onPasteAndRizzClick = onPasteAndRizz,
            onSuggestionClick = onSuggestionSelected,
            onDismissSuggestions = onDismissSuggestions,
            onDismissWarning = onDismissWarning,
            onOpenSettings = onOpenSettings
        )

        // Soft Keyboard Matrix
        KeyboardLayout(
            layoutMode = layoutMode,
            imeOptions = imeOptions,
            onKeyClick = { key ->
                onKeyTyped(key)
                // If in single-upper shift mode, reset back to lower after typing 1 letter
                if (layoutMode == KeyboardLayoutMode.QWERTY_UPPER) {
                    layoutMode = KeyboardLayoutMode.QWERTY_LOWER
                    shiftTapCount = 0
                }
            },
            onBackspaceClick = onBackspace,
            onSpaceClick = onSpace,
            onActionClick = onAction,
            onShiftClick = {
                shiftTapCount++
                layoutMode = when {
                    shiftTapCount >= 2 -> KeyboardLayoutMode.CAPS_LOCK
                    layoutMode == KeyboardLayoutMode.QWERTY_LOWER -> KeyboardLayoutMode.QWERTY_UPPER
                    else -> {
                        shiftTapCount = 0
                        KeyboardLayoutMode.QWERTY_LOWER
                    }
                }
            },
            onToggleSymbolsClick = {
                layoutMode = when (layoutMode) {
                    KeyboardLayoutMode.QWERTY_LOWER,
                    KeyboardLayoutMode.QWERTY_UPPER,
                    KeyboardLayoutMode.CAPS_LOCK -> KeyboardLayoutMode.SYMBOLS_1
                    else -> KeyboardLayoutMode.QWERTY_LOWER
                }
                shiftTapCount = 0
            },
            onHapticFeedback = onHapticFeedback
        )
    }
}

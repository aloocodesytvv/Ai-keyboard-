package com.example.keyboard

import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActionKeyBackground
import com.example.ui.theme.KeyBackgroundDark
import com.example.ui.theme.KeyBackgroundPressedDark
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonIndigo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun KeyboardLayout(
    layoutMode: KeyboardLayoutMode,
    imeOptions: Int,
    onKeyClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onActionClick: () -> Unit,
    onShiftClick: () -> Unit,
    onToggleSymbolsClick: () -> Unit,
    onHapticFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (layoutMode) {
            KeyboardLayoutMode.QWERTY_LOWER,
            KeyboardLayoutMode.QWERTY_UPPER,
            KeyboardLayoutMode.CAPS_LOCK -> {
                QwertyRows(
                    layoutMode = layoutMode,
                    imeOptions = imeOptions,
                    onKeyClick = onKeyClick,
                    onBackspaceClick = onBackspaceClick,
                    onSpaceClick = onSpaceClick,
                    onActionClick = onActionClick,
                    onShiftClick = onShiftClick,
                    onToggleSymbolsClick = onToggleSymbolsClick,
                    onHapticFeedback = onHapticFeedback
                )
            }
            KeyboardLayoutMode.SYMBOLS_1,
            KeyboardLayoutMode.SYMBOLS_2 -> {
                SymbolsRows(
                    layoutMode = layoutMode,
                    imeOptions = imeOptions,
                    onKeyClick = onKeyClick,
                    onBackspaceClick = onBackspaceClick,
                    onSpaceClick = onSpaceClick,
                    onActionClick = onActionClick,
                    onToggleSymbolsClick = onToggleSymbolsClick,
                    onHapticFeedback = onHapticFeedback
                )
            }
        }
    }
}

@Composable
private fun QwertyRows(
    layoutMode: KeyboardLayoutMode,
    imeOptions: Int,
    onKeyClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onActionClick: () -> Unit,
    onShiftClick: () -> Unit,
    onToggleSymbolsClick: () -> Unit,
    onHapticFeedback: () -> Unit
) {
    val isUpper = layoutMode == KeyboardLayoutMode.QWERTY_UPPER || layoutMode == KeyboardLayoutMode.CAPS_LOCK
    val isCapsLock = layoutMode == KeyboardLayoutMode.CAPS_LOCK

    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    // Row 1: q w e r t y u i o p
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row1.forEach { key ->
            val charToEmit = if (isUpper) key.uppercase() else key
            KeyboardKey(
                label = charToEmit,
                modifier = Modifier.weight(1f),
                onClick = {
                    onHapticFeedback()
                    onKeyClick(charToEmit)
                }
            )
        }
    }

    // Row 2: a s d f g h j k l (with horizontal padding for standard staggered QWERTY look)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row2.forEach { key ->
            val charToEmit = if (isUpper) key.uppercase() else key
            KeyboardKey(
                label = charToEmit,
                modifier = Modifier.weight(1f),
                onClick = {
                    onHapticFeedback()
                    onKeyClick(charToEmit)
                }
            )
        }
    }

    // Row 3: Shift, z x c v b n m, Backspace
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shift Key
        ActionKey(
            modifier = Modifier.weight(1.5f),
            backgroundColor = if (isUpper) NeonIndigo else ActionKeyBackground,
            testTag = "key_shift",
            onClick = {
                onHapticFeedback()
                onShiftClick()
            }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = if (isCapsLock) "Caps Lock Active" else "Shift",
                tint = if (isCapsLock) NeonGold else Color.White
            )
        }

        row3.forEach { key ->
            val charToEmit = if (isUpper) key.uppercase() else key
            KeyboardKey(
                label = charToEmit,
                modifier = Modifier.weight(1f),
                onClick = {
                    onHapticFeedback()
                    onKeyClick(charToEmit)
                }
            )
        }

        // Backspace Key with repeat on hold
        RepeatingActionKey(
            modifier = Modifier.weight(1.5f),
            testTag = "key_backspace",
            onAction = {
                onHapticFeedback()
                onBackspaceClick()
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Backspace",
                tint = Color.White
            )
        }
    }

    // Row 4: ?123, comma, spacebar, period, Enter/Send
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ?123 toggle
        ActionKey(
            modifier = Modifier.weight(1.4f),
            testTag = "key_symbols_toggle",
            onClick = {
                onHapticFeedback()
                onToggleSymbolsClick()
            }
        ) {
            Text(
                text = "?123",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        // Comma
        KeyboardKey(
            label = ",",
            modifier = Modifier.weight(1f),
            onClick = {
                onHapticFeedback()
                onKeyClick(",")
            }
        )

        // Spacebar
        SpacebarKey(
            modifier = Modifier.weight(4.5f),
            onClick = {
                onHapticFeedback()
                onSpaceClick()
            }
        )

        // Period
        KeyboardKey(
            label = ".",
            modifier = Modifier.weight(1f),
            onClick = {
                onHapticFeedback()
                onKeyClick(".")
            }
        )

        // Enter / Action Key
        ImeActionKey(
            imeOptions = imeOptions,
            modifier = Modifier.weight(1.5f),
            onClick = {
                onHapticFeedback()
                onActionClick()
            }
        )
    }
}

@Composable
private fun SymbolsRows(
    layoutMode: KeyboardLayoutMode,
    imeOptions: Int,
    onKeyClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onActionClick: () -> Unit,
    onToggleSymbolsClick: () -> Unit,
    onHapticFeedback: () -> Unit
) {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    val row3 = listOf("=", "<", ">", "*", "\"", "'", ":", ";", "!", "?")

    // Row 1
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row1.forEach { key ->
            KeyboardKey(
                label = key,
                modifier = Modifier.weight(1f),
                onClick = {
                    onHapticFeedback()
                    onKeyClick(key)
                }
            )
        }
    }

    // Row 2
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        row2.forEach { key ->
            KeyboardKey(
                label = key,
                modifier = Modifier.weight(1f),
                onClick = {
                    onHapticFeedback()
                    onKeyClick(key)
                }
            )
        }
    }

    // Row 3
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        row3.forEach { key ->
            KeyboardKey(
                label = key,
                modifier = Modifier.weight(1f),
                onClick = {
                    onHapticFeedback()
                    onKeyClick(key)
                }
            )
        }

        RepeatingActionKey(
            modifier = Modifier.weight(1.3f),
            testTag = "key_backspace_sym",
            onAction = {
                onHapticFeedback()
                onBackspaceClick()
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Backspace",
                tint = Color.White
            )
        }
    }

    // Row 4
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ABC toggle
        ActionKey(
            modifier = Modifier.weight(1.4f),
            testTag = "key_abc_toggle",
            onClick = {
                onHapticFeedback()
                onToggleSymbolsClick()
            }
        ) {
            Text(
                text = "ABC",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        KeyboardKey(
            label = ",",
            modifier = Modifier.weight(1f),
            onClick = {
                onHapticFeedback()
                onKeyClick(",")
            }
        )

        SpacebarKey(
            modifier = Modifier.weight(4.5f),
            onClick = {
                onHapticFeedback()
                onSpaceClick()
            }
        )

        KeyboardKey(
            label = ".",
            modifier = Modifier.weight(1f),
            onClick = {
                onHapticFeedback()
                onKeyClick(".")
            }
        )

        ImeActionKey(
            imeOptions = imeOptions,
            modifier = Modifier.weight(1.5f),
            onClick = {
                onHapticFeedback()
                onActionClick()
            }
        )
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) KeyBackgroundPressedDark else KeyBackgroundDark)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White
        )
    }
}

@Composable
private fun SpacebarKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) KeyBackgroundPressedDark else KeyBackgroundDark)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .testTag("key_space"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "RizzBoard AI",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun ActionKey(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ActionKeyBackground,
    testTag: String = "",
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) KeyBackgroundPressedDark else backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun RepeatingActionKey(
    modifier: Modifier = Modifier,
    testTag: String = "",
    onAction: () -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) KeyBackgroundPressedDark else ActionKeyBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onAction() // First trigger immediately
                        repeatJob = scope.launch {
                            delay(400) // Initial hold delay
                            while (isActive) {
                                onAction()
                                delay(60) // Rapid repeat rate
                            }
                        }
                        tryAwaitRelease()
                        repeatJob?.cancel()
                        isPressed = false
                    }
                )
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ImeActionKey(
    imeOptions: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val action = imeOptions and EditorInfo.IME_MASK_ACTION

    val isSendOrSearch = action == EditorInfo.IME_ACTION_SEND ||
            action == EditorInfo.IME_ACTION_SEARCH ||
            action == EditorInfo.IME_ACTION_GO ||
            action == EditorInfo.IME_ACTION_DONE

    val bgColor = if (isSendOrSearch) NeonIndigo else ActionKeyBackground

    ActionKey(
        modifier = modifier,
        backgroundColor = bgColor,
        testTag = "key_ime_action",
        onClick = onClick
    ) {
        when (action) {
            EditorInfo.IME_ACTION_SEARCH -> {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
            EditorInfo.IME_ACTION_SEND -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
            EditorInfo.IME_ACTION_DONE -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.White
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.KeyboardReturn,
                    contentDescription = "Enter / Return",
                    tint = Color.White
                )
            }
        }
    }
}

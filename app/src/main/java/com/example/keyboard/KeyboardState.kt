package com.example.keyboard

import com.example.ai.ToneTransformation

enum class KeyboardLayoutMode {
    QWERTY_LOWER,
    QWERTY_UPPER,
    CAPS_LOCK,
    SYMBOLS_1,
    SYMBOLS_2
}

sealed class AiToolbarState {
    data object Default : AiToolbarState()
    data class Loading(val message: String) : AiToolbarState()
    data class Suggestions(val options: List<String>, val originalMessage: String) : AiToolbarState()
    data class Warning(val message: String, val isApiKeyMissing: Boolean = false) : AiToolbarState()
}

package com.example.keyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ToneTransformation
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonGoldGlow
import com.example.ui.theme.NeonIndigo

@Composable
fun AiToolbar(
    state: AiToolbarState,
    onToneClick: (ToneTransformation) -> Unit,
    onPasteAndRizzClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDismissSuggestions: () -> Unit,
    onDismissWarning: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("ai_toolbar_container"),
        color = Color(0xFF131A29),
        tonalElevation = 4.dp
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "AiToolbarContent"
        ) { targetState ->
            when (targetState) {
                is AiToolbarState.Default -> {
                    DefaultToolbarRow(
                        onToneClick = onToneClick,
                        onPasteAndRizzClick = onPasteAndRizzClick,
                        onOpenSettings = onOpenSettings
                    )
                }

                is AiToolbarState.Loading -> {
                    LoadingToolbarRow(
                        message = targetState.message,
                        onCancel = onDismissSuggestions
                    )
                }

                is AiToolbarState.Suggestions -> {
                    SuggestionsToolbarRow(
                        options = targetState.options,
                        onSuggestionClick = onSuggestionClick,
                        onClose = onDismissSuggestions
                    )
                }

                is AiToolbarState.Warning -> {
                    WarningToolbarRow(
                        message = targetState.message,
                        isApiKeyMissing = targetState.isApiKeyMissing,
                        onOpenSettings = onOpenSettings,
                        onDismiss = onDismissWarning
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultToolbarRow(
    onToneClick: (ToneTransformation) -> Unit,
    onPasteAndRizzClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Settings quick link icon
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .testTag("btn_keyboard_settings")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "RizzBoard Settings",
                tint = NeonGold,
                modifier = Modifier.size(18.dp)
            )
        }

        // 1. Smooth 🌟
        ToolbarChip(
            label = ToneTransformation.SMOOTH.label,
            gradient = Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
            testTag = "chip_smooth",
            onClick = { onToneClick(ToneTransformation.SMOOTH) }
        )

        // 2. Witty 🔥
        ToolbarChip(
            label = ToneTransformation.WITTY.label,
            gradient = Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444))),
            testTag = "chip_witty",
            onClick = { onToneClick(ToneTransformation.WITTY) }
        )

        // 3. Chill 🧊
        ToolbarChip(
            label = ToneTransformation.CHILL.label,
            gradient = Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
            testTag = "chip_chill",
            onClick = { onToneClick(ToneTransformation.CHILL) }
        )

        // 4. Victorian 📜
        ToolbarChip(
            label = ToneTransformation.VICTORIAN.label,
            gradient = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFB45309))),
            testTag = "chip_victorian",
            onClick = { onToneClick(ToneTransformation.VICTORIAN) }
        )

        // 5. Fix Grammar ✍️
        ToolbarChip(
            label = ToneTransformation.FIX_GRAMMAR.label,
            gradient = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
            testTag = "chip_grammar",
            onClick = { onToneClick(ToneTransformation.FIX_GRAMMAR) }
        )

        // 6. Gen Z 🧢
        ToolbarChip(
            label = ToneTransformation.GEN_Z.label,
            gradient = Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1))),
            testTag = "chip_genz",
            onClick = { onToneClick(ToneTransformation.GEN_Z) }
        )

        // 7. Paste & Rizz 📋 (Highlighted special action)
        ToolbarChip(
            label = "Paste & Rizz 📋",
            gradient = Brush.horizontalGradient(listOf(Color(0xFFFFB800), Color(0xFFFF5722))),
            textColor = Color.Black,
            isBold = true,
            testTag = "chip_paste_rizz",
            onClick = onPasteAndRizzClick
        )
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    isBold: Boolean = false,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun LoadingToolbarRow(
    message: String,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = NeonGold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NeonGoldGlow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SuggestionsToolbarRow(
    options: List<String>,
    onSuggestionClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Badge indicator
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF312E81))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Pick 1 of 3 ✨",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGold
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Horizontal scroll of the 3 suggestion chips
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF334155))
                            )
                        )
                        .clickable { onSuggestionClick(option) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("suggestion_chip_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss Suggestions",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WarningToolbarRow(
    message: String,
    isApiKeyMissing: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable {
                    if (isApiKeyMissing) {
                        onOpenSettings()
                    } else {
                        onDismiss()
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Notice",
                tint = if (isApiKeyMissing) NeonGold else Color(0xFFEF4444),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isApiKeyMissing) NeonGoldGlow else Color(0xFFFCA5A5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isApiKeyMissing) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonIndigo)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Settings",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss notice",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

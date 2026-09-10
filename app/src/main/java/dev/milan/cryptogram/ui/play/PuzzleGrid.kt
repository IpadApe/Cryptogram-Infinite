package dev.milan.cryptogram.ui.play

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.milan.cryptogram.engine.PuzzleState
import dev.milan.cryptogram.engine.PuzzleStatus

/**
 * Renders the ciphertext as tappable letter cells, wrapping whole words as units
 * (design doc sections 7-8). Cell size shrinks to fit the widest word. Wrong cells
 * shake; on solve every cell flips to primaryContainer with a short stagger.
 */
@Composable
fun PuzzleGrid(
    state: PuzzleState,
    onCellClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lockedChars = state.solvableCipherChars
        .filter { correctPlain(state, it) in state.revealed }
        .toSet()

    val words = splitWords(state.cipher)
    val longestWord = (words.maxOfOrNull { it.length } ?: 1).coerceAtLeast(1)
    val solved = state.status == PuzzleStatus.SOLVED

    BoxWithConstraints(modifier) {
        val gap = 2.dp
        val maxCell = 30.dp
        val fitted = (maxWidth - gap * (longestWord - 1)) / longestWord
        val cellWidth: Dp = fitted.coerceIn(16.dp, maxCell)

        Column(Modifier.verticalScroll(rememberScrollState())) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                var flatIndex = 0
                words.forEach { word ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        word.forEach { ch ->
                            if (ch in 'A'..'Z') {
                                val index = flatIndex++
                                LetterCell(
                                    cipherChar = ch,
                                    guess = state.mapping[ch],
                                    locked = ch in lockedChars,
                                    wrong = ch in state.wrongCipherChars,
                                    selected = ch == state.selectedCipherChar,
                                    solved = solved,
                                    solveDelayMs = index * 20,
                                    width = cellWidth,
                                    onClick = { onCellClick(ch) },
                                )
                            } else {
                                Text(
                                    ch.toString(),
                                    modifier = Modifier
                                        .width(cellWidth * 0.5f)
                                        .padding(top = 16.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterCell(
    cipherChar: Char,
    guess: Char?,
    locked: Boolean,
    wrong: Boolean,
    selected: Boolean,
    solved: Boolean,
    solveDelayMs: Int,
    width: Dp,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val target = when {
        solved -> scheme.primaryContainer
        wrong -> scheme.errorContainer
        locked -> scheme.secondaryContainer
        selected -> scheme.primaryContainer
        else -> scheme.surface
    }
    val bg by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 180, delayMillis = if (solved) solveDelayMs else 0),
        label = "cellBg",
    )

    // Wrong-letter shake: 8dp, ~300ms.
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(wrong) {
        if (wrong) {
            val px = with(density) { 8.dp.toPx() }
            shake.snapTo(0f)
            repeat(3) {
                shake.animateTo(px, tween(50, easing = LinearEasing))
                shake.animateTo(-px, tween(50, easing = LinearEasing))
            }
            shake.animateTo(0f, tween(50, easing = LinearEasing))
        }
    }

    val desc = when {
        guess != null && locked -> "Cipher $cipherChar, locked as $guess"
        guess != null -> "Cipher $cipherChar, guess $guess"
        else -> "Cipher $cipherChar, empty"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(width)
            .graphicsLayer { translationX = shake.value }
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, scheme.outline, RoundedCornerShape(4.dp))
            .let { if (locked) it else it.clickable(onClick = onClick) }
            .semantics { contentDescription = desc }
            .padding(vertical = 2.dp),
    ) {
        Text((guess ?: ' ').toString(), style = MaterialTheme.typography.titleMedium)
        Text(
            cipherChar.toString(),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = scheme.onSurfaceVariant,
        )
    }
}

private fun correctPlain(state: PuzzleState, cipherChar: Char): Char {
    val idx = state.key.indexOf(cipherChar)
    return if (idx >= 0) 'A' + idx else cipherChar
}

private fun splitWords(cipher: String): List<String> {
    val out = mutableListOf<String>()
    val sb = StringBuilder()
    for (ch in cipher) {
        if (ch == ' ') {
            if (sb.isNotEmpty()) { out.add(sb.toString()); sb.clear() }
        } else {
            sb.append(ch)
        }
    }
    if (sb.isNotEmpty()) out.add(sb.toString())
    return out
}

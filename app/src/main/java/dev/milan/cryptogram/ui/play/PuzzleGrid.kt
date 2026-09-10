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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import dev.milan.cryptogram.engine.CipherToken
import dev.milan.cryptogram.engine.PuzzleState
import dev.milan.cryptogram.engine.PuzzleStatus

/**
 * Renders the ciphertext as tappable cells, one number per letter, wrapping whole
 * words as units (design doc sections 7-8). Symbol cells are inert; pre-revealed
 * cells are locked; wrong cells shake; on solve every cell flips to
 * primaryContainer with a short stagger.
 */
@Composable
fun PuzzleGrid(
    state: PuzzleState,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inv = remember(state.key) {
        IntArray(27).also { for (i in 0 until 26) it[state.key[i]] = i }
    }
    fun correctPlain(n: Int): Char = 'A' + inv[n]

    val lockedNums = state.solvableCipherNums
        .filter { correctPlain(it) in state.revealed }
        .toSet()

    val words = splitWords(state.tokens())
    val longestWord = (words.maxOfOrNull { it.size } ?: 1).coerceAtLeast(1)
    val solved = state.status == PuzzleStatus.SOLVED

    BoxWithConstraints(modifier) {
        val gap = 3.dp
        val maxCell = 34.dp
        val fitted = (maxWidth - gap * (longestWord - 1)) / longestWord
        val cellWidth: Dp = fitted.coerceIn(22.dp, maxCell)

        Column(Modifier.verticalScroll(rememberScrollState())) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                var flatIndex = 0
                words.forEach { word ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        word.forEach { token ->
                            when (token) {
                                is CipherToken.Num -> {
                                    val index = flatIndex++
                                    NumberCell(
                                        number = token.n,
                                        guess = state.mapping[token.n],
                                        locked = token.n in lockedNums,
                                        wrong = token.n in state.wrongCipherNums,
                                        selected = token.n == state.selectedCipherNum,
                                        solved = solved,
                                        solveDelayMs = index * 20,
                                        width = cellWidth,
                                        onClick = { onCellClick(token.n) },
                                    )
                                }

                                is CipherToken.Sym -> Text(
                                    token.c.toString(),
                                    modifier = Modifier
                                        .width(cellWidth * 0.4f)
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
private fun NumberCell(
    number: Int,
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
        guess != null && locked -> "Number $number, locked as $guess"
        guess != null -> "Number $number, guess $guess"
        else -> "Number $number, empty"
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
            number.toString(),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = scheme.onSurfaceVariant,
        )
    }
}

private fun splitWords(tokens: List<CipherToken>): List<List<CipherToken>> {
    val out = mutableListOf<List<CipherToken>>()
    var cur = mutableListOf<CipherToken>()
    for (t in tokens) {
        if (t is CipherToken.Sym && t.c == ' ') {
            if (cur.isNotEmpty()) { out.add(cur); cur = mutableListOf() }
        } else {
            cur.add(t)
        }
    }
    if (cur.isNotEmpty()) out.add(cur)
    return out
}

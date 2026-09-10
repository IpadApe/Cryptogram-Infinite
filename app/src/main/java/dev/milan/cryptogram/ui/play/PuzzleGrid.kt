package dev.milan.cryptogram.ui.play

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.milan.cryptogram.engine.CipherToken
import dev.milan.cryptogram.engine.PuzzleState
import dev.milan.cryptogram.engine.PuzzleStatus
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

/**
 * The ciphertext as tappable tiles, whole words wrapped as units (design canvas
 * "Working prototype"). The number always sits under the tile as a caption; the
 * tile box holds only the guessed letter (in serif) once the player fills it.
 * Given tiles are green; a wrong guess shakes, flashes red and clears itself.
 */
@Composable
fun PuzzleGrid(
    state: PuzzleState,
    onTileClick: (num: Int, position: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CryptoTheme.colors
    val inv = remember(state.key) {
        IntArray(27).also { for (i in 0 until 26) it[state.key[i]] = i }
    }
    fun correctPlain(n: Int): Char = 'A' + inv[n]

    val lockedNums = state.solvableCipherNums
        .filter { correctPlain(it) in state.revealed }
        .toSet()
    val words = splitWords(state.tokens())
    val solved = state.status == PuzzleStatus.SOLVED

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            var flatIndex = 0
            words.forEach { word ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    word.forEach { token ->
                        when (token) {
                            is CipherToken.Num -> {
                                val i = flatIndex++
                                val filled = state.isPositionFilled(i)
                                Tile(
                                    number = token.n,
                                    letter = if (filled) state.mapping[token.n] else null,
                                    locked = token.n in lockedNums,
                                    flashWrong = token.n == state.lastWrongNum,
                                    selected = token.n == state.selectedCipherNum,
                                    solved = solved,
                                    solveDelayMs = i * 20,
                                    onClick = { onTileClick(token.n, i) },
                                )
                            }

                            is CipherToken.Sym -> Text(
                                token.c.toString(),
                                modifier = Modifier.padding(top = 4.dp),
                                fontFamily = Serif,
                                fontSize = 19.sp,
                                color = c.ink,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Tile(
    number: Int,
    letter: Char?,
    locked: Boolean,
    flashWrong: Boolean,
    selected: Boolean,
    solved: Boolean,
    solveDelayMs: Int,
    onClick: () -> Unit,
) {
    val c = CryptoTheme.colors
    val hasGuess = letter != null

    // A same-number tile that is still empty: highlight it so tapping a
    // correctly-placed letter shows where else that number goes.
    val emptyMate = selected && !hasGuess && !solved && !flashWrong

    val tileBg = when {
        emptyMate -> c.accent.copy(alpha = 0.28f)
        selected && !solved -> c.accent.copy(alpha = 0.16f)
        !hasGuess && !solved -> c.tileUnsolved
        else -> Color.Transparent
    }
    val bg by animateColorAsState(
        if (solved) c.accent.copy(alpha = 0.16f) else tileBg,
        tween(180, delayMillis = if (solved) solveDelayMs else 0),
        label = "tileBg",
    )
    val underline = when {
        flashWrong -> c.bad
        locked || selected -> c.accent
        else -> c.divider
    }
    val glyphColor = when {
        flashWrong -> c.bad
        hasGuess && locked -> c.accent
        hasGuess -> c.ink
        selected -> c.accent
        else -> c.muted
    }

    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(flashWrong) {
        if (flashWrong) {
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
        hasGuess && locked -> "Number $number, given as $letter"
        hasGuess -> "Number $number, guess $letter"
        else -> "Number $number, empty"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(22.dp)
            .graphicsLayer { translationX = shake.value }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = desc },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(27.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(bg)
                .let {
                    if (emptyMate) it.border(1.5.dp, c.accent, RoundedCornerShape(3.dp)) else it
                },
            contentAlignment = Alignment.Center,
        ) {
            if (hasGuess && !flashWrong) {
                Text(letter.toString(), fontFamily = Serif, fontSize = 19.sp, color = glyphColor)
            }
        }
        Spacer(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(underline),
        )
        // The number always sits under the letter.
        Text(
            number.toString().padStart(2, '0'),
            modifier = Modifier.padding(top = 3.dp),
            fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 8.5.sp,
            color = when {
                flashWrong -> c.bad
                selected || locked -> c.accent
                else -> c.muted
            },
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

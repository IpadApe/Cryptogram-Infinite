package dev.milan.cryptogram.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.milan.cryptogram.engine.PuzzleState

/**
 * Renders the ciphertext as tappable letter cells, wrapping whole words as units
 * (design doc section 8, Play). Punctuation cells are inert; pre-revealed cells are
 * locked; wrong cells are tinted; every occurrence of the selected cipher letter is
 * highlighted.
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

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                words.forEach { word ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        word.forEach { ch ->
                            if (ch in 'A'..'Z') {
                                LetterCell(
                                    cipherChar = ch,
                                    guess = state.mapping[ch],
                                    locked = ch in lockedChars,
                                    wrong = ch in state.wrongCipherChars,
                                    selected = ch == state.selectedCipherChar,
                                    onClick = { onCellClick(ch) },
                                )
                            } else {
                                Text(
                                    ch.toString(),
                                    modifier = Modifier
                                        .width(10.dp)
                                        .padding(top = 18.dp),
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
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bg = when {
        wrong -> scheme.errorContainer
        locked -> scheme.secondaryContainer
        selected -> scheme.primaryContainer
        else -> scheme.surface
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, scheme.outline, RoundedCornerShape(4.dp))
            .let { if (locked) it else it.clickable(onClick = onClick) }
            .padding(vertical = 2.dp),
    ) {
        Text(
            (guess ?: ' ').toString(),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            cipherChar.toString(),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = scheme.onSurfaceVariant,
        )
    }
}

private fun correctPlain(state: PuzzleState, cipherChar: Char): Char {
    // key[i] = cipher char for plaintext 'A'+i; invert to recover the plaintext letter.
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

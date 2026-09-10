package dev.milan.cryptogram.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono

private val ROWS = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
private val ROW_PAD = listOf(0.dp, 18.dp, 46.dp)

/**
 * QWERTY letter keyboard on a paper tray, plus DELETE / NEXT NUMBER
 * (design canvas). No system IME.
 *
 * [placedLetters]: assigned to a number that still has an empty tile — shown
 * green, still tappable. [doneLetters]: assigned and every tile of its number
 * filled (no free slot left) — dimmed. Both come straight from the puzzle state,
 * so the keyboard resets with each level.
 */
@Composable
fun CipherKeyboard(
    placedLetters: Set<Char>,
    doneLetters: Set<Char>,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onNextNumber: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CryptoTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.keyboardBg)
            .padding(start = 5.dp, end = 5.dp, top = 11.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ROWS.forEachIndexed { i, row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ROW_PAD[i]),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                row.forEach { ch ->
                    val done = ch in doneLetters
                    val placed = ch in placedLetters && !done
                    Key(
                        modifier = Modifier.weight(1f),
                        label = ch.toString(),
                        bg = when {
                            placed -> c.accent.copy(alpha = 0.16f)
                            done -> c.keyUsed
                            else -> c.key
                        },
                        fg = when {
                            placed -> c.accent
                            done -> c.ink.copy(alpha = 0.3f)
                            else -> c.ink
                        },
                        onClick = { onKey(ch) },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Key(
                modifier = Modifier.weight(1f),
                label = "DELETE",
                bg = c.keySpecial,
                fg = c.ink,
                small = true,
                onClick = onBackspace,
            )
            Key(
                modifier = Modifier.weight(1f),
                label = "NEXT NUMBER",
                bg = c.keySpecial,
                fg = c.ink,
                small = true,
                onClick = onNextNumber,
            )
        }
    }
}

@Composable
private fun Key(
    modifier: Modifier,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    small: Boolean = false,
) {
    Box(
        modifier
            .height(if (small) 42.dp else 44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = Mono,
            fontWeight = FontWeight.Medium,
            fontSize = if (small) 10.sp else 16.sp,
            letterSpacing = if (small) 0.16.em else 0.em,
            color = fg,
        )
    }
}

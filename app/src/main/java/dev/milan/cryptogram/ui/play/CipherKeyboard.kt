package dev.milan.cryptogram.ui.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val ROWS = listOf("ABCDEFGHI", "JKLMNOPQR", "STUVWXYZ")

/**
 * On-screen A–Z keyboard plus backspace, Hint and (Hard) Check (design doc section 8).
 * No system IME is used.
 */
@Composable
fun CipherKeyboard(
    usedLetters: Set<Char>,
    canCheck: Boolean,
    hintLabel: String,
    hintEnabled: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onHint: () -> Unit,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ROWS.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { c ->
                    val used = c in usedLetters
                    val keyModifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Letter $c" }
                    if (used) {
                        OutlinedButton(
                            onClick = { onKey(c) },
                            modifier = keyModifier,
                            contentPadding = PaddingZero,
                        ) { Text(c.toString()) }
                    } else {
                        FilledTonalButton(
                            onClick = { onKey(c) },
                            modifier = keyModifier,
                            contentPadding = PaddingZero,
                        ) { Text(c.toString()) }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedButton(
                onClick = onBackspace,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Backspace" },
            ) { Text("⌫") }
            Button(
                onClick = onHint,
                enabled = hintEnabled,
                modifier = Modifier.weight(1.4f),
            ) { Text(hintLabel) }
            if (canCheck) {
                Button(onClick = onCheck, modifier = Modifier.weight(1f)) { Text("Check") }
            }
        }
    }
}

private val PaddingZero = androidx.compose.foundation.layout.PaddingValues(0.dp)

package dev.milan.cryptogram.ui.sources

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

private const val WIKIQUOTE_LICENSE = "https://creativecommons.org/licenses/by-sa/3.0/"
private const val CONTENT_REPO = "https://github.com/IpadApe/Cryptogram-Infinite"

@Composable
fun SourcesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sources", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Quotes are reproduced verbatim with attribution. Text is sourced from " +
                "Wikiquote and Wikisource and is licensed CC BY-SA 3.0.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, WIKIQUOTE_LICENSE.toUri()))
        }) { Text("CC BY-SA 3.0 licence") }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, CONTENT_REPO.toUri()))
        }) { Text("Content repository") }
    }
}

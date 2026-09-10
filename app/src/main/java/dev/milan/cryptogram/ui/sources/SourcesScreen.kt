package dev.milan.cryptogram.ui.sources

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import dev.milan.cryptogram.ui.components.BackHeader
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Serif

private const val WIKIQUOTE_LICENSE = "https://creativecommons.org/licenses/by-sa/3.0/"
private const val CONTENT_REPO = "https://github.com/IpadApe/Cryptogram-Infinite"

@Composable
fun SourcesScreen(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val c = CryptoTheme.colors
    val context = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, start = 26.dp, end = 26.dp, bottom = 40.dp),
    ) {
        BackHeader("Sources", onBack)
        Spacer(Modifier.height(20.dp))
        Text(
            "Quotes are reproduced verbatim with attribution. Text is sourced from " +
                "Wikiquote and Wikisource and is licensed CC BY-SA 3.0.",
            fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 16.sp, lineHeight = 25.sp,
            color = c.ink,
        )
        Spacer(Modifier.height(20.dp))
        LinkRow("CC BY-SA 3.0 licence") {
            context.startActivity(Intent(Intent.ACTION_VIEW, WIKIQUOTE_LICENSE.toUri()))
        }
        LinkRow("Content repository") {
            context.startActivity(Intent(Intent.ACTION_VIEW, CONTENT_REPO.toUri()))
        }
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val c = CryptoTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 16.sp, color = c.ink)
        Spacer(Modifier.weight(1f))
        Text("›", fontFamily = Serif, fontSize = 18.sp, color = c.muted)
    }
}

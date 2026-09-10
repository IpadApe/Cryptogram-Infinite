package dev.milan.cryptogram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono

/** Full-bleed paper background with the design's status-bar inset. */
@Composable
fun PaperScreen(
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 26,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = CryptoTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .padding(top = 56.dp, start = horizontalPadding.dp, end = horizontalPadding.dp),
        content = content,
    )
}

/** Wide-tracked uppercase mono caption. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    Text(
        text.uppercase(),
        modifier = modifier,
        fontFamily = Mono,
        fontWeight = if (strong) FontWeight.Medium else FontWeight.Normal,
        fontSize = if (strong) 10.5.sp else 9.5.sp,
        letterSpacing = 0.18.em,
        color = CryptoTheme.colors.muted,
    )
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = CryptoTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "‹",
            fontSize = 28.sp,
            color = c.ink,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
        )
        SectionLabel(title, strong = true)
    }
}

@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = CryptoTheme.colors
    var m = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(c.card)
        .border(1.dp, c.divider, RoundedCornerShape(10.dp))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(18.dp), content = content)
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CryptoTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.5.sp,
            letterSpacing = 0.18.em, color = c.onAccent,
        )
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = CryptoTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, c.ink.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp,
            letterSpacing = 0.16.em, color = c.ink,
        )
    }
}

@Composable
fun PaperDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CryptoTheme.colors.divider),
    )
}

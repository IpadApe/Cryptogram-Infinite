package dev.milan.cryptogram.ui.play

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.engine.CipherToken
import dev.milan.cryptogram.engine.Difficulty
import dev.milan.cryptogram.engine.FeedbackMode
import dev.milan.cryptogram.engine.PuzzleState
import dev.milan.cryptogram.engine.PuzzleStatus
import dev.milan.cryptogram.ui.rememberAppContainer
import dev.milan.cryptogram.ui.results.ResultArgs
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono

@Composable
fun PlayScreen(
    difficulty: Difficulty,
    level: Int,
    onSolved: (ResultArgs) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    dailyDate: String? = null,
    viewModel: PlayViewModel = viewModel(
        factory = PlayViewModel.factory(difficulty, level, dailyDate),
        key = "play-${dailyDate ?: "level"}-$difficulty-$level",
    ),
) {
    val c = CryptoTheme.colors
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val container = rememberAppContainer()
    val rewardedHint = remember { container.newRewardedHintAd() }
    val rewardedLoaded by rewardedHint.loaded.collectAsStateWithLifecycle()
    val soundEnabled by container.settingsStore.soundEnabled
        .collectAsStateWithLifecycle(initialValue = true)
    val hapticsEnabled by container.settingsStore.hapticsEnabled
        .collectAsStateWithLifecycle(initialValue = true)
    val feedback = rememberPlayFeedback(soundEnabled, hapticsEnabled)
    var freqOpen by remember { mutableStateOf(true) }
    var keyboardOpen by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { rewardedHint.load() }
    LaunchedEffect(rewardedLoaded) { viewModel.setAdHintLoaded(rewardedLoaded) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setResumed(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setResumed(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is PlayEvent.NavigateToResults -> onSolved(event.args)
                PlayEvent.LoadFailed -> onExit()
            }
        }
    }

    val state = ui
    if (state == null) {
        Box(modifier.fillMaxSize().background(c.paper), Alignment.Center) { CircularProgressIndicator(color = c.accent) }
        return
    }
    val puzzle = state.puzzle

    val lastMistakes = remember { mutableIntStateOf(puzzle.mistakes) }
    LaunchedEffect(puzzle.mistakes) {
        if (puzzle.mistakes > lastMistakes.intValue) feedback.onWrong()
        lastMistakes.intValue = puzzle.mistakes
    }
    LaunchedEffect(puzzle.status) {
        if (puzzle.status == PuzzleStatus.SOLVED) feedback.onSolve()
    }

    val baseDensity = LocalDensity.current
    val clampedDensity = remember(baseDensity) {
        Density(baseDensity.density, baseDensity.fontScale.coerceAtMost(1.3f))
    }

    // number -> occurrence count, and number -> is it correctly solved
    val counts = remember(puzzle.plain, puzzle.key) {
        val m = HashMap<Int, Int>()
        puzzle.tokens().forEach { if (it is CipherToken.Num) m[it.n] = (m[it.n] ?: 0) + 1 }
        m
    }
    val inv = remember(puzzle.key) { IntArray(27).also { for (i in 0 until 26) it[puzzle.key[i]] = i } }
    fun correct(n: Int) = 'A' + inv[n]
    val orderedNums = remember(counts) { counts.keys.sortedByDescending { counts[it] } }

    val filled = puzzle.solvableCipherNums.count { it in puzzle.mapping }
    val pct = if (puzzle.solvableCipherNums.isEmpty()) 0f
        else filled.toFloat() / puzzle.solvableCipherNums.size

    // Keyboard letter state — only the letters the player *correctly* placed
    // (given/hinted letters, and any wrong-but-filled guess on ON_CHECK/
    // ON_COMPLETE bands, are excluded), derived from the puzzle so it resets
    // every level: green while a correct letter still has an empty tile,
    // hidden entirely once every tile for it is correctly filled.
    val placedLetters = remember(puzzle) {
        buildSet {
            puzzle.mapping.forEach { (num, letter) ->
                if (letter in puzzle.revealed) return@forEach
                if (letter != correct(num)) return@forEach
                val positions = puzzle.letterNums.withIndex().filter { it.value == num }.map { it.index }
                if (positions.any { !puzzle.isPositionFilled(it) }) add(letter)
            }
        }
    }
    val doneLetters = remember(puzzle) {
        buildSet {
            puzzle.mapping.forEach { (num, letter) ->
                if (letter in puzzle.revealed) return@forEach
                if (letter != correct(num)) return@forEach
                val positions = puzzle.letterNums.withIndex().filter { it.value == num }.map { it.index }
                if (positions.isNotEmpty() && positions.all { puzzle.isPositionFilled(it) }) add(letter)
            }
        }
    }

    val bandLabel = state.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
    val selNum = puzzle.selectedCipherNum
    val selLabel = if (selNum != null) {
        val n = counts[selNum] ?: 0
        "NUMBER ${selNum.toString().padStart(2, '0')} · $n ${if (n == 1) "PLACE" else "PLACES"}"
    } else {
        "TAP A TILE TO PICK A NUMBER"
    }

    fun doReveal() {
        if (state.canHint) viewModel.hint(fromAd = false)
        else if (state.canAdHint) (context as? Activity)?.let { a ->
            rewardedHint.show(a) { viewModel.hint(fromAd = true) }
        }
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        Column(modifier.fillMaxSize().background(c.paper)) {
            Spacer(Modifier.height(52.dp))

            // header
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "‹",
                    fontSize = 28.sp,
                    color = c.ink,
                    modifier = Modifier
                        .clickable(remember { MutableInteractionSource() }, null) { onExit() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${if (state.isDaily) "DAILY · ${bandLabel.uppercase()}" else "NO. $level"} · ${formatTime(puzzle.elapsedMs)}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = c.muted,
                )
                Spacer(Modifier.weight(1f))
                Pill("♥ ${puzzle.livesLeft}", filled = false)
            }

            // progress
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(2.dp).background(c.divider),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pct).background(c.accent))
            }

            PuzzleGrid(
                state = puzzle,
                onTileClick = { _, position ->
                    feedback.onTap(); viewModel.selectAt(position); keyboardOpen = true
                },
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 30.dp),
            )

            // frequency drawer
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(remember { MutableInteractionSource() }, null) { freqOpen = !freqOpen }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (freqOpen) "▾" else "▸", fontFamily = Mono, fontSize = 10.sp, color = c.muted)
                    Spacer(Modifier.width(7.dp))
                    Text("NUMBER FREQUENCY", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = c.muted)
                    Spacer(Modifier.weight(1f))
                    Text("TAP A BAR TO SELECT", fontFamily = Mono, fontSize = 8.5.sp, color = c.muted.copy(alpha = 0.7f))
                }
                if (freqOpen) {
                    Row(
                        Modifier.fillMaxWidth().height(46.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                        orderedNums.forEach { n ->
                            val done = puzzle.mapping[n] == correct(n)
                            val isSel = n == selNum
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clickable(remember { MutableInteractionSource() }, null) {
                                        feedback.onTap(); viewModel.select(n)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                val h = (8 + ((counts[n] ?: 0).toFloat() / max) * 24).dp
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(h)
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        .background(
                                            when {
                                                isSel -> c.accent
                                                done -> c.accent.copy(alpha = 0.4f)
                                                else -> c.ink.copy(alpha = 0.18f)
                                            }
                                        ),
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    n.toString(),
                                    fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 8.5.sp,
                                    color = if (isSel) c.accent else c.muted,
                                )
                            }
                        }
                    }
                }
            }

            // selection label + action pills
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selLabel,
                    fontFamily = Mono, fontSize = 10.5.sp,
                    color = if (selNum != null) c.accent else c.muted,
                )
                Spacer(Modifier.weight(1f))
                if (state.difficulty.feedback == FeedbackMode.ON_CHECK && state.canCheck) {
                    Pill("CHECK", filled = false, onClick = viewModel::check)
                    Spacer(Modifier.width(8.dp))
                }
                if (state.canHint || state.canAdHint) {
                    Pill(
                        if (state.canHint) "REVEAL · ${puzzle.hintsLeft}" else "REVEAL · AD",
                        filled = true,
                        onClick = ::doReveal,
                    )
                }
            }

            // keyboard show/hide bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(c.keyboardBg)
                    .clickable(remember { MutableInteractionSource() }, null) { keyboardOpen = !keyboardOpen }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (keyboardOpen) "▾" else "▸", fontFamily = Mono, fontSize = 10.sp, color = c.muted)
                Spacer(Modifier.width(7.dp))
                Text(
                    if (keyboardOpen) "HIDE KEYBOARD" else "SHOW KEYBOARD",
                    fontFamily = Mono, fontSize = 8.5.sp, letterSpacing = 0.18.em, color = c.muted,
                )
            }

            if (keyboardOpen) {
                CipherKeyboard(
                    placedLetters = placedLetters,
                    doneLetters = doneLetters,
                    onKey = { ch -> feedback.onTap(); viewModel.enter(ch) },
                    onBackspace = viewModel::clearCell,
                    onNextNumber = viewModel::nextNumber,
                )
            }
        }
    }

    if (state.showFailedDialog && puzzle.status == PuzzleStatus.FAILED) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = c.card,
            titleContentColor = c.ink,
            textContentColor = c.muted,
            title = { Text("OUT OF LIVES", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) },
            text = { Text("The puzzle restarts with a fresh code.", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = viewModel::tryAgain) { Text("TRY AGAIN", color = c.accent, fontFamily = Mono, fontSize = 11.sp) } },
            dismissButton = { TextButton(onClick = onExit) { Text("LEAVE", color = c.muted, fontFamily = Mono, fontSize = 11.sp) } },
        )
    }
}

@Composable
private fun Pill(text: String, filled: Boolean, onClick: (() -> Unit)? = null) {
    val c = CryptoTheme.colors
    val shape = RoundedCornerShape(999.dp)
    var m = Modifier
        .height(31.dp)
        .clip(shape)
        .background(if (filled) c.accent.copy(alpha = 0.12f) else Color.Transparent)
    if (!filled) m = m.border(1.dp, c.ink.copy(alpha = 0.18f), shape)
    if (onClick != null) m = m.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
    Box(m.padding(horizontal = 13.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.sp,
            color = if (filled) c.accent else c.muted,
        )
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

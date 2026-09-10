package dev.milan.cryptogram.engine

import kotlinx.serialization.Serializable

/**
 * The four fixed difficulty bands. Values are the single source of truth for band length
 * ranges, reveal ratios, lives, free hints and feedback timing (design doc section 4.1).
 */
@Serializable
enum class Difficulty(
    val minLen: Int,
    val maxLen: Int,
    val revealRatio: Float,
    val lives: Int,
    val freeHints: Int,
    val feedback: FeedbackMode,
) {
    EASY(20, 30, 0.60f, 5, 4, FeedbackMode.IMMEDIATE),
    MEDIUM(31, 45, 0.45f, 4, 3, FeedbackMode.IMMEDIATE),
    HARD(46, 70, 0.30f, 3, 2, FeedbackMode.ON_CHECK),
    EXTREME(71, 100, 0.15f, 3, 1, FeedbackMode.ON_COMPLETE),
}

enum class FeedbackMode { IMMEDIATE, ON_CHECK, ON_COMPLETE }

const val MAX_AD_HINTS_PER_PUZZLE = 3

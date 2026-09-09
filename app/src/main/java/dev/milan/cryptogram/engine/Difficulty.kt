package dev.milan.cryptogram.engine

/**
 * The four fixed difficulty bands. Values are the single source of truth for band length
 * ranges, reveal ratios, lives, free hints and feedback timing (design doc section 4.1).
 */
enum class Difficulty(
    val minLen: Int,
    val maxLen: Int,
    val revealRatio: Float,
    val lives: Int,
    val freeHints: Int,
    val feedback: FeedbackMode,
) {
    EASY(20, 30, 0.50f, 5, 3, FeedbackMode.IMMEDIATE),
    MEDIUM(31, 45, 0.30f, 4, 2, FeedbackMode.IMMEDIATE),
    HARD(46, 70, 0.15f, 3, 1, FeedbackMode.ON_CHECK),
    EXTREME(71, 100, 0.05f, 3, 0, FeedbackMode.ON_COMPLETE),
}

enum class FeedbackMode { IMMEDIATE, ON_CHECK, ON_COMPLETE }

const val MAX_AD_HINTS_PER_PUZZLE = 3

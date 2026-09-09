package dev.milan.cryptogram.ui.results

import android.util.Base64
import dev.milan.cryptogram.engine.Difficulty
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class PlayKind { LEVEL, DAILY }

/** Everything the Results screen needs, passed as one encoded route argument. */
@Serializable
data class ResultArgs(
    val kind: PlayKind,
    val difficulty: Difficulty,
    val level: Int? = null,
    val date: String? = null,
    val quoteId: Int,
    val timeMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val stars: Int,
) {
    fun encode(): String {
        val json = Json.encodeToString(serializer(), this)
        return Base64.encodeToString(
            json.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    companion object {
        fun decode(raw: String): ResultArgs {
            val json = String(
                Base64.decode(raw, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP),
                Charsets.UTF_8,
            )
            return Json.decodeFromString(serializer(), json)
        }
    }
}

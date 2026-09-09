package dev.milan.cryptogram.data.corpus

import dev.milan.cryptogram.data.db.entities.QuoteEntity
import kotlinx.serialization.Serializable

/** Shape of `assets/corpus.json`, produced by the content repo's build_assets.py. */
@Serializable
data class CorpusFile(
    val version: Int,
    val quotes: List<CorpusQuote>,
)

@Serializable
data class CorpusQuote(
    val id: Int,
    val text: String,
    val author: String,
    val source: String = "",
    val sourceUrl: String = "",
    val band: String,
    val addedOn: String? = null,
) {
    fun toEntity() = QuoteEntity(
        id = id,
        text = text,
        author = author,
        source = source,
        sourceUrl = sourceUrl,
        band = band,
    )
}

package dev.milan.cryptogram.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.data.corpus.QuoteRepository
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResultsUiState(
    val args: ResultArgs,
    val quoteText: String = "",
    val author: String = "",
    val source: String = "",
    val sourceUrl: String = "",
)

class ResultsViewModel(
    private val args: ResultArgs,
    private val quotes: QuoteRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResultsUiState(args))
    val state: StateFlow<ResultsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val q = quotes.byId(args.quoteId)
            if (q != null) {
                _state.value = _state.value.copy(
                    quoteText = q.text,
                    author = q.author,
                    source = q.source,
                    sourceUrl = q.sourceUrl,
                )
            }
        }
    }

    companion object {
        fun factory(args: ResultArgs) = viewModelFactory {
            initializer { ResultsViewModel(args, appContainer.quoteRepository) }
        }
    }
}

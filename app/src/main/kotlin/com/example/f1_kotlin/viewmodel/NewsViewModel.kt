package com.example.f1_kotlin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.f1_kotlin.data.model.NewsArticle
import com.example.f1_kotlin.data.repository.IEspnRepository
import com.example.f1_kotlin.domain.AppDataRefresh
import com.example.f1_kotlin.domain.AsyncValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NewsUiState(
    val articles: AsyncValue<List<NewsArticle>> = AsyncValue.Loading,
    val isRefreshing: Boolean = false,
)

/** ViewModel вкладки «Новости» (ESPN). */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val espnRepository: IEspnRepository,
    private val appDataRefresh: AppDataRefresh,
) : ViewModel() {
    private val loadJob = LoadJobHolder()

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles(forceRefresh: Boolean = false) {
        loadJob.launch(viewModelScope) {
            if (forceRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
                appDataRefresh.clearAll()
            }
            try {
                if (!forceRefresh) {
                    espnRepository.peekNews?.let { cached ->
                        _uiState.update { it.copy(articles = AsyncValue.Value(cached)) }
                        if (espnRepository.isNewsFresh) return@launch
                    } ?: run {
                        _uiState.update { it.copy(articles = AsyncValue.Loading) }
                    }
                } else if (_uiState.value.articles !is AsyncValue.Value) {
                    _uiState.update { it.copy(articles = AsyncValue.Loading) }
                }

                espnRepository.getNews(forceRefresh = forceRefresh).applyUnlessCached(
                    current = _uiState.value.articles,
                    onSuccess = { list ->
                        _uiState.update { it.copy(articles = AsyncValue.Value(list)) }
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(articles = err.toAsyncError()) }
                    },
                )
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun refreshAll() = loadArticles(forceRefresh = true)
}

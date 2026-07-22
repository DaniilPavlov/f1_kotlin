package com.example.f1_kotlin.ui.screens.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.f1_kotlin.R
import com.example.f1_kotlin.domain.AsyncValue
import com.example.f1_kotlin.ui.components.ErrorBody
import com.example.f1_kotlin.ui.components.NewsArticleTile
import com.example.f1_kotlin.ui.components.shimmer.NewsListShimmer
import com.example.f1_kotlin.ui.theme.AppDimens
import com.example.f1_kotlin.ui.theme.AppStyles
import com.example.f1_kotlin.viewmodel.NewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(viewModel: NewsViewModel) {
    val articles by viewModel.articles.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()

    when (val state = articles) {
        is AsyncValue.Loading -> NewsListShimmer(modifier = Modifier.fillMaxSize())
        is AsyncValue.Error -> ErrorBody(
            state.message,
            state.subtitle,
            onRetry = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        )
        is AsyncValue.Value -> {
            val list = state.value
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refreshAll,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.news_empty), style = AppStyles.body)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = AppDimens.horizontalPadding.dp,
                            vertical = 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(list, key = { "${it.id}-${it.webUrl}" }) { article ->
                            NewsArticleTile(article)
                        }
                        item { Spacer(Modifier.height(AppDimens.verticalPadding.dp)) }
                    }
                }
            }
        }
    }
}

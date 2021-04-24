package com.github.kittinunf.app.knews.screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ListItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.kittinunf.app.knews.ui.theme.typography
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.detail.DetailUiCommentRowState
import com.github.kittinunf.hackernews.api.detail.DetailUiState
import com.github.kittinunf.hackernews.api.detail.DetailUiStoryState
import com.github.kittinunf.hackernews.api.detail.HackerNewsDetailViewModel
import com.github.kittinunf.hackernews.api.detail.HackerNewsDetailViewModelFactory
import com.github.kittinunf.hackernews.repository.HackerNewsService

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun KNewsDetailScreen(detailUiState: DetailUiState, service: HackerNewsService) {
    val viewModel = viewModel<HackerNewsDetailViewModel>(factory = HackerNewsDetailViewModelFactory(service))

    val states by viewModel.states.collectAsState(rememberCoroutineScope().coroutineContext)

    when (val story = detailUiState.story) {
        is Data.Success -> viewModel.setInitialStory(story.value)
        else -> viewModel.loadStory()
    }

    viewModel.loadStoryComments()

    BackdropScaffold(appBar = {},
        scaffoldState = rememberBackdropScaffoldState(BackdropValue.Revealed),
        frontLayerScrimColor = Color.Transparent,
        backLayerBackgroundColor = Color.Transparent,
        backLayerContent = {
            when (val story = states.story) {
                is Data.Loading -> LoadingComponent()
                is Data.Success -> {
                    StoryComponent(state = story.value)
                }
                is Data.Failure -> {
                    ErrorComponent { viewModel.loadStory() }
                }
            }
        },
        frontLayerContent = {
            val bottomSheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White, shape = bottomSheetShape) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(modifier = Modifier.align(Alignment.CenterHorizontally), imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)

                    when (val comments = states.comments) {
                        is Data.Loading -> LoadingComponent()
                        is Data.Success -> {
                            CommentComponent(comments = comments.value)
                        }
                        is Data.Failure -> {
                            ErrorComponent { viewModel.loadStoryComments() }
                        }
                    }
                }
            }
        })
}

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StoryComponent(state: DetailUiStoryState) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = ::WebView
    ) { view ->
        with(view) {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {}

                override fun onPageFinished(view: WebView?, url: String?) {}
            }
            loadUrl(state.url.toString())
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CommentComponent(comments: List<DetailUiCommentRowState>) {
    Text(
        text = "Comments: ${comments.size}",
        style = typography.caption
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn {
        itemsIndexed(items = comments, key = null, itemContent = { _, rowState ->
            Column {
                ListItem(text = { CommentRowComponent(rowState.text) },
                    overlineText = {
                        Text(
                            text = "${rowState.by} | ${rowState.fromNowText}",
                            style = typography.overline
                        )
                    })

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, bottom = 4.dp, end = 8.dp)
                )
            }
        })
    }
}

@Composable
private fun CommentRowComponent(text: String) {
    val comment = if (text.isEmpty()) "( Comment is deleted )..." else text
    val style = if (text.isEmpty()) typography.subtitle1.copy(Color.LightGray) else typography.subtitle1
    Text(modifier = Modifier.padding(vertical = 4.dp), text = comment, style = style)
}

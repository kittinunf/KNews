package com.github.kittinunf.app.knews

import androidx.activity.addCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import com.github.kittinunf.app.knews.screen.KNewsDetailScreen
import com.github.kittinunf.app.knews.screen.KNewsListScreen
import com.github.kittinunf.app.knews.ui.theme.KNewsColor
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.HackerNewsDependency
import com.github.kittinunf.hackernews.api.detail.DetailUiState
import com.github.kittinunf.hackernews.api.detail.DetailUiStoryState
import com.github.kittinunf.hackernews.repository.HackerNewsServiceImpl
import io.ktor.http.Url

sealed class NavigationState {
    object ListScreen : NavigationState()
    data class DetailScreen(val id: Int, val title: String, val url: Url, val commentIds: List<Int>? = null, val descendants: Int? = null) : NavigationState()
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScaffold() {
    val service = HackerNewsServiceImpl(HackerNewsDependency.networkModule)

    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.ListScreen) }
    var isSortButtonSelected by remember { mutableStateOf(false) }

    LocalOnBackPressedDispatcherOwner.current.onBackPressedDispatcher.apply {
        addCallback(owner = LocalLifecycleOwner.current, enabled = true) {
            isEnabled = navigationState is NavigationState.DetailScreen
            navigationState = NavigationState.ListScreen
        }
    }

    Scaffold(backgroundColor = KNewsColor.tan,
        topBar = {
            TopAppBar(title = {
                when (val state = navigationState) {
                    is NavigationState.DetailScreen -> Text(text = state.title, maxLines = 1,overflow = TextOverflow.Ellipsis)
                    else -> Text(text = "KNews")
                }
            }, actions = {
                AnimatedVisibility(visible = navigationState == NavigationState.ListScreen) {
                    SortToggleButtonComponent(isSelected = isSortButtonSelected, onClick = {
                        isSortButtonSelected = !isSortButtonSelected
                    })
                }
            })
        },
        content = {
            Crossfade(targetState = navigationState) {
                when (val navigation = it) {
                    NavigationState.ListScreen -> {
                        KNewsListScreen(
                            isSortSelected = isSortButtonSelected,
                            onSortSelected = {
                                isSortButtonSelected = !isSortButtonSelected
                            },
                            onStoryClick = { state ->
                                navigationState = NavigationState.DetailScreen(state.id, state.title, state.url, state.commentIds, state.descendants)
                            },
                            service = service
                        )
                    }
                    is NavigationState.DetailScreen -> {
                        KNewsDetailScreen(
                            detailUiState = DetailUiState(
                                navigation.id, Data.Success(
                                    DetailUiStoryState(
                                        navigation.id,
                                        navigation.title,
                                        navigation.url,
                                        navigation.commentIds,
                                        navigation.descendants
                                    )
                                )
                            ),
                            service = service
                        )
                    }
                }
            }
        })
}

@Composable
fun SortToggleButtonComponent(isSelected: Boolean, onClick: () -> Unit) {
    IconToggleButton(checked = isSelected, onCheckedChange = {
        onClick()
    }, content = {
        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
    })
}

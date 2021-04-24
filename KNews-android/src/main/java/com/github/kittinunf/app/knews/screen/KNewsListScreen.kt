package com.github.kittinunf.app.knews.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.kittinunf.app.knews.ui.theme.typography
import com.github.kittinunf.hackernews.api.Data
import com.github.kittinunf.hackernews.api.list.HackerNewsListViewModel
import com.github.kittinunf.hackernews.api.list.HackerNewsListViewModelFactory
import com.github.kittinunf.hackernews.api.list.ListUiRowState
import com.github.kittinunf.hackernews.api.list.ListUiSortCondition
import com.github.kittinunf.hackernews.repository.HackerNewsService

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun KNewsListScreen(isSortSelected: Boolean, onSortSelected: () -> Unit, onStoryClick: (ListUiRowState) -> Unit, service: HackerNewsService) {
    val viewModel = viewModel<HackerNewsListViewModel>(factory = HackerNewsListViewModelFactory(service))

    val states by viewModel.states.collectAsState(rememberCoroutineScope().coroutineContext)

    when (val stories = states.stories) {
        is Data.Initial -> {
            viewModel.loadStories()
        }
        is Data.Loading -> LoadingComponent()
        is Data.Success ->
            SortOverlayComponent(selected = isSortSelected,
                currentSortingCondition = states.sortCondition,
                onSortConditionSelected = { sortCondition ->
                    onSortSelected()
                    viewModel.sortBy(sortCondition)
                    if (sortCondition == ListUiSortCondition.None) { //if the sortCondition is none, then we load everything again from the api
                        viewModel.loadStories()
                    }
                },
                content = {
                    StoryListComponent(rowStates = stories.value,
                        onStoryClick = { onStoryClick(it) },
                        onLoadNext = {
                            viewModel.loadNextStories()
                        })
                })
        is Data.Failure -> {
            ErrorComponent { viewModel.loadStories() }
        }
    }

    AnimatedVisibility(visible = states.nextStories is Data.Loading) {
        Row(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SortOverlayComponent(selected: Boolean, currentSortingCondition: ListUiSortCondition, onSortConditionSelected: (ListUiSortCondition) -> Unit, content: @Composable () -> Unit) {
    Box {
        content()
        // it has to be on top of the content provided
        AnimatedVisibility(visible = selected) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White),
                verticalArrangement = Arrangement.Center
            ) {
                ListUiSortCondition.values().forEach { sortCondition ->
                    SortOverlayItemComponent(text = sortCondition.name, selected = currentSortingCondition == sortCondition) {
                        onSortConditionSelected(sortCondition)
                    }
                }
            }
        }
    }
}

@Composable
fun SortOverlayItemComponent(text: String, selected: Boolean, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(modifier = Modifier.padding(8.dp), imageVector = Icons.Default.Check, contentDescription = null)
        }
        Text(
            modifier = Modifier.padding(8.dp),
            text = text,
            style = typography.subtitle1.copy(background = Color.White),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StoryListComponent(rowStates: List<ListUiRowState>, onStoryClick: (ListUiRowState) -> Unit, onLoadNext: @Composable () -> Unit) {
    val lastIndex = rowStates.lastIndex

    LazyColumn {
        itemsIndexed(items = rowStates, key = { _, rowState -> rowState.id }, itemContent = { index, rowState ->
            Card(
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color.White,
                modifier = Modifier
                    .fillParentMaxWidth()
                    .padding(8.dp)
                    .clickable { onStoryClick(rowState) }
            ) {
                Column {
                    ListItem(
                        modifier = Modifier.padding(4.dp),
                        text = {
                            Text(
                                text = rowState.title,
                                style = typography.h5
                            )
                        },
                        secondaryText = {
                            Text(
                                text = "by: ${rowState.by} | ${rowState.fromNowText}",
                                style = typography.subtitle1
                            )
                        },
                        overlineText = {
                            Text(
                                // we don't wanna show the "http://www part
                                text = "(${rowState.url.host.substringAfter("www.")})",
                                style = typography.overline
                            )
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconAndTextComponent(image = Icons.Default.Star, text = rowState.score.toString())
                        IconAndTextComponent(image = Icons.Default.Person, text = (rowState.commentIds?.size ?: 0).toString())
                    }
                }
            }

            if (index == lastIndex) {
                onLoadNext()
            }
        })
    }
}

@Composable
fun IconAndTextComponent(modifier: Modifier = Modifier, image: ImageVector, text: String) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = image, contentDescription = null, tint = Color.DarkGray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = typography.subtitle1, color = Color.DarkGray)
    }
}

@Composable
fun LoadingComponent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally))
    }
}

@Composable
fun ErrorComponent(onClick: () -> Unit) {
    IconButton(modifier = Modifier.fillMaxSize(), onClick = { onClick() }) {
        IconAndTextComponent(image = Icons.Default.Warning, text = "Error, please tap to reload")
    }
}

package com.baltajmn.flowtime.features.screens.onboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R.drawable.ic_flowtime
import com.baltajmn.flowtime.core.design.R.drawable.ic_list
import com.baltajmn.flowtime.core.design.R.drawable.ic_music
import com.baltajmn.flowtime.core.design.R.drawable.ic_percentage
import com.baltajmn.flowtime.core.design.R.drawable.ic_pomodoro
import com.baltajmn.flowtime.core.design.R.drawable.ic_settings
import com.baltajmn.flowtime.core.design.R.string.on_board_finish
import com.baltajmn.flowtime.core.design.R.string.on_board_next
import com.baltajmn.flowtime.core.design.R.string.on_board_skip
import com.baltajmn.flowtime.core.design.R.string.on_board_subtitle_1
import com.baltajmn.flowtime.core.design.R.string.on_board_subtitle_2
import com.baltajmn.flowtime.core.design.R.string.on_board_subtitle_3
import com.baltajmn.flowtime.core.design.R.string.on_board_subtitle_4
import com.baltajmn.flowtime.core.design.R.string.on_board_subtitle_5
import com.baltajmn.flowtime.core.design.R.string.on_board_subtitle_6
import com.baltajmn.flowtime.core.design.R.string.on_board_title_1
import com.baltajmn.flowtime.core.design.R.string.on_board_title_2
import com.baltajmn.flowtime.core.design.R.string.on_board_title_3
import com.baltajmn.flowtime.core.design.R.string.on_board_title_4
import com.baltajmn.flowtime.core.design.R.string.on_board_title_5
import com.baltajmn.flowtime.core.design.R.string.on_board_title_6
import com.baltajmn.flowtime.core.design.components.collectEvents
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.NavigateToMainGraph
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnBoardScreen(
    viewModel: OnBoardViewModel = koinViewModel(),
    navigateToMainGraph: () -> Unit
) {
    collectEvents {
        viewModel.event.collectLatest {
            when (it) {
                is NavigateToMainGraph -> navigateToMainGraph()
            }
        }
    }

    OnboardingContent(viewModel = viewModel)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingContent(
    viewModel: OnBoardViewModel,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_1),
            description = LocalContext.current.getString(on_board_subtitle_1),
            imageRes = ic_flowtime
        ),
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_1),
            description = LocalContext.current.getString(on_board_subtitle_1),
            imageRes = ic_flowtime
        ),
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_2),
            description = LocalContext.current.getString(on_board_subtitle_2),
            imageRes = ic_pomodoro
        ),
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_3),
            description = LocalContext.current.getString(on_board_subtitle_3),
            imageRes = ic_percentage
        ),
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_4),
            description = LocalContext.current.getString(on_board_subtitle_4),
            imageRes = ic_list
        ),
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_6),
            description = LocalContext.current.getString(on_board_subtitle_6),
            imageRes = ic_music
        ),
        OnBoardModel(
            title = LocalContext.current.getString(on_board_title_5),
            description = LocalContext.current.getString(on_board_subtitle_5),
            imageRes = ic_settings
        )
    )

    val pagerState = rememberPagerState(
        pageCount = { pages.size },
        initialPage = 0
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnBoardItem(pages[page])
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)

        ) {
            Text(
                modifier = Modifier.clickable {
                    val skipPage = pagerState.pageCount - 1
                    coroutineScope.launch { pagerState.animateScrollToPage(skipPage) }
                },
                text = LocalContext.current.getString(on_board_skip),
                style = Title.copy(
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .width(if (isSelected) 18.dp else 8.dp)
                            .height(if (isSelected) 8.dp else 8.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                shape = CircleShape
                            )
                    )
                }
            }

            Text(
                modifier = Modifier.clickable {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.finishOnBoard()
                    }
                },
                text = LocalContext.current.getString(
                    if (pagerState.currentPage < pages.size - 1) on_board_next else on_board_finish
                ),
                style = Title.copy(
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun OnBoardItem(page: OnBoardModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .height(350.dp)
                .width(350.dp)
                .padding(bottom = 20.dp),
            painter = painterResource(id = page.imageRes),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
            contentDescription = null
        )
        Text(
            text = page.title,
            style = LargeTitle.copy(
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary
            )
        )
        Text(
            text = page.description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = Title.copy(
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

data class OnBoardModel(
    val imageRes: Int,
    val title: String,
    val description: String
)
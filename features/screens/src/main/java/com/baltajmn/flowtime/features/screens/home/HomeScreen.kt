package com.baltajmn.flowtime.features.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R.drawable.ic_flowtime
import com.baltajmn.flowtime.core.design.R.drawable.ic_percentage
import com.baltajmn.flowtime.core.design.R.drawable.ic_pomodoro
import com.baltajmn.flowtime.core.design.R.string.flow_time_title
import com.baltajmn.flowtime.core.design.R.string.percentage_title
import com.baltajmn.flowtime.core.design.R.string.pomodoro_title
import com.baltajmn.flowtime.core.design.extensions.noRippleClickable
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.design.model.ScreenType.FlowTime
import com.baltajmn.flowtime.core.design.model.ScreenType.Percentage
import com.baltajmn.flowtime.core.design.model.ScreenType.Pomodoro
import com.baltajmn.flowtime.core.design.theme.Body
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle

@Composable
fun HomeScreen(navigateToScreen: (ScreenType) -> Unit) =
    HomeContent(navigateToScreen = navigateToScreen)

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    navigateToScreen: (ScreenType) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item { HomeTitle() }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        items(ScreenType.entries) {
            Spacer(modifier = Modifier.height(32.dp))
            ScreenCard(screenType = it, navigateToScreen = navigateToScreen)
        }
    }
}

@Composable
fun HomeTitle() {
    Text(text = "Home", style = LargeTitle.copy(color = MaterialTheme.colorScheme.primary))
}

@Composable
fun ScreenCard(screenType: ScreenType, navigateToScreen: (ScreenType) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable { navigateToScreen.invoke(screenType) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Icon(
                modifier = Modifier
                    .size(64.dp)
                    .padding(
                        bottom = 8.dp
                    ),
                painter = painterResource(getIcon(screenType = screenType)),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = ""
            )
            Text(text = stringResource(getTitle(screenType = screenType)), style = SmallTitle)
            Text(text = stringResource(getDescription(screenType = screenType)), style = Body)
        }
    }
}

private fun getIcon(screenType: ScreenType) = when (screenType) {
    Pomodoro -> ic_pomodoro
    FlowTime -> ic_flowtime
    Percentage -> ic_percentage
}

private fun getTitle(screenType: ScreenType) = when (screenType) {
    Pomodoro -> pomodoro_title
    FlowTime -> flow_time_title
    Percentage -> percentage_title
}

private fun getDescription(screenType: ScreenType) = when (screenType) {
    Pomodoro -> pomodoro_title
    FlowTime -> flow_time_title
    Percentage -> percentage_title
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun HomeScreenPreview() {
    Scaffold {
        HomeContent(navigateToScreen = { })
    }
}
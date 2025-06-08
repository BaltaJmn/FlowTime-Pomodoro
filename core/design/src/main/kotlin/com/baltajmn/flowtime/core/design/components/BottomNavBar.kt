package com.baltajmn.flowtime.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.navigation.MainGraph
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun BottomNavBar(
    modifier: Modifier = Modifier,
    currentRoute: () -> String,
    onSelectedItem: (BottomNavBarItem, ScreenType) -> Unit,
    shouldShow: () -> Boolean
) {

    var firstVisibility by rememberSaveable {
        mutableStateOf(false)
    }

    var isTimerScreen = currentRoute() in setOf(
        MainGraph.Pomodoro.route,
        MainGraph.FlowTime.route,
        MainGraph.Percentage.route
    )

    var isEditScreen = currentRoute() == MainGraph.Edit.route

    var itemsToShow = when {
        isTimerScreen -> {
            listOf(
                BottomNavBarItem.Back,
                BottomNavBarItem.Edit
            )
        }

        isEditScreen -> {
            listOf(
                BottomNavBarItem.Back
            )
        }

        else -> listOf(
            BottomNavBarItem.Home,
            BottomNavBarItem.TodoList,
            BottomNavBarItem.Settings
        )
    }

    LaunchedEffect(Unit) {
        delay(200)
        firstVisibility = true
    }

    AnimatedVisibility(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        visible = shouldShow() && firstVisibility,
        enter = slideInVertically(
            initialOffsetY = { 1000 },
            animationSpec = tween(
                durationMillis = 400,
                easing = LinearEasing
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { 1000 },
            animationSpec = tween(
                durationMillis = 400,
                easing = LinearEasing
            )
        )
    ) {
        BottomBarSurface(
            modifier = modifier.height(IntrinsicSize.Min)
        ) {
            itemsToShow.forEach {
                BottomBarButton(
                    navItem = it,
                    currentRoute = currentRoute(),
                    isSelected = { currentRoute().contains(it.getScreenRoute()) },
                    onSelectedItem = onSelectedItem
                )
            }
        }
    }
}

@Composable
fun BottomBarSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Surface(
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = modifier
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(20)
                )
                .align(Alignment.Center)
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun BottomBarButton(
    modifier: Modifier = Modifier,
    navItem: BottomNavBarItem,
    currentRoute: String,
    isSelected: () -> Boolean,
    onSelectedItem: (BottomNavBarItem, ScreenType) -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val ripple = rememberRipple(bounded = false, color = primaryColor)

    val transition = updateTransition(targetState = isSelected, label = null)

    val iconColor by transition.animateColor(label = "") {
        if (it()) primaryColor else MaterialTheme.colorScheme.secondary
    }

    val dothAlpha by transition.animateFloat(label = "") {
        if (it()) 1f else 0f
    }

    Box(
        modifier = modifier
            .padding(10.dp)
            .selectable(
                selected = isSelected(),
                onClick = { onSelectedItem(navItem, currentRoute.toScreenType()) },
                enabled = true,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = ripple
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = navItem.icon),
                contentDescription = navItem.name,
                modifier = Modifier
                    .drawBehind {
                        drawCircle(
                            color = primaryColor,
                            alpha = dothAlpha,
                            radius = 2.3.dp.toPx(),
                            style = Fill,
                            center = Offset(
                                size.width / 2,
                                (size.height + 3.dp.toPx()) + ((1f - dothAlpha) * 48.dp.toPx())
                            )
                        )
                    }
                    .offset { IntOffset(x = 0, y = ((-dothAlpha) * 10).roundToInt()) },
                colorFilter = ColorFilter.tint(iconColor),
            )

        }
    }
}


@Preview(showBackground = true)
@Composable
fun BottomNavBarPreview() {
    BottomNavBar(
        currentRoute = { BottomNavBarItem.Home.getScreenRoute() },
        onSelectedItem = { _, _ -> },
        shouldShow = { true }
    )
}

enum class BottomNavBarItem(val icon: Int) {
    Edit(icon = R.drawable.ic_edit),
    Back(icon = R.drawable.ic_back),
    Home(icon = R.drawable.ic_home),
    TodoList(icon = R.drawable.ic_list),
    Settings(icon = R.drawable.ic_settings);

    fun getScreenRoute() = when (this) {
        Edit -> MainGraph.Edit.route

        Back,
        Home -> MainGraph.Home.route

        TodoList -> MainGraph.TodoList.route
        Settings -> MainGraph.Settings.route
    }

}

fun String.toScreenType(): ScreenType {
    return when (this) {
        MainGraph.Pomodoro.route -> ScreenType.Pomodoro
        MainGraph.FlowTime.route -> ScreenType.FlowTime
        MainGraph.Percentage.route -> ScreenType.Percentage
        else -> ScreenType.Pomodoro
    }
}
package com.baltajmn.flowtime.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.service.PlayerState
import com.baltajmn.flowtime.core.design.service.PlayerType
import com.baltajmn.flowtime.core.design.service.PlayerType.BROWN
import com.baltajmn.flowtime.core.design.service.PlayerType.PINK
import com.baltajmn.flowtime.core.design.service.PlayerType.WHITE
import com.baltajmn.flowtime.core.design.service.SoundViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel


@Composable
fun TopNavBar(
    modifier: Modifier = Modifier,
    viewModel: SoundViewModel = koinViewModel(),
    shouldShow: () -> Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var firstVisibility by rememberSaveable { mutableStateOf(false) }

    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f
    )

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                viewModel.muteAll()
            }

            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        firstVisibility = true
    }

    AnimatedVisibility(
        modifier = modifier
            .navigationBarsPadding()
            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 96.dp),
        visible = shouldShow() && firstVisibility,
        enter = slideInHorizontally(
            initialOffsetX = { 1000 },
            animationSpec = tween(
                durationMillis = 400,
                easing = LinearEasing
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { 1000 },
            animationSpec = tween(
                durationMillis = 400,
                easing = LinearEasing
            )
        )
    ) {
        TopBarSurface(
            modifier = modifier
                .animateContentSize()
                .wrapContentWidth(),
            expanded = expanded
        ) {
            CurrentlyPlaying(
                onExpandedClick = { expanded = !expanded },
                rotationState = rotationState
            )

            if (expanded) {
                ExpandedContent(
                    items = viewModel.getItems(),
                    onPlayClicked = { playerType, playing ->
                        viewModel.controlSounds(playerType = playerType, playing = playing)
                    },
                    onVolumeChanged = { itemPlayer, volume ->
                        viewModel.setVolume(
                            type = itemPlayer,
                            volume = volume
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun CurrentlyPlaying(
    onExpandedClick: () -> Unit,
    rotationState: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        LottieImage(
            modifier = Modifier.size(30.dp),
            animation = R.raw.equalizer,
            tintColor = MaterialTheme.colorScheme.secondary
        )

        IconButton(
            modifier = Modifier.rotate(rotationState),
            onClick = onExpandedClick
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = "Drop-Down Arrow"
            )
        }
    }
}

@Composable
fun ExpandedContent(
    items: MutableMap<PlayerType, PlayerState>,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.Top
    ) {
        items(
            items = PlayerType.entries,
            key = { it.sound }
        ) {
            SliderItem(
                type = it,
                playing = items[it]?.isPlaying ?: false,
                volume = items[it]?.volume ?: 0f,
                onPlayClicked = onPlayClicked,
                onVolumeChanged = onVolumeChanged
            )
        }
    }
}

@Composable
fun SliderItem(
    type: PlayerType,
    playing: Boolean,
    volume: Float,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit
) {
    var sliderPlaying by rememberSaveable(playing) { mutableStateOf(playing) }
    var sliderVolume by rememberSaveable(volume) { mutableFloatStateOf(volume) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Icon(
            painter = painterResource(type.icon),
            contentDescription = "",
            tint = if (type != BROWN && type != PINK && type != WHITE) {
                MaterialTheme.colorScheme.secondary
            } else {
                Color.Unspecified
            }
        )

        Spacer(modifier = Modifier.size(10.dp))

        Slider(
            modifier = Modifier
                .padding(10.dp)
                .weight(1f),
            value = sliderVolume,
            onValueChange = {
                sliderVolume = it
                onVolumeChanged(type, it)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
            )
        )

        Spacer(modifier = Modifier.size(10.dp))

        IconButton(
            onClick = {
                sliderPlaying = !sliderPlaying
                onPlayClicked(type, sliderPlaying)
            }
        ) {
            Icon(
                painter = painterResource(if (sliderPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = "Play/Pause Button"
            )
        }
    }
}

@Composable
fun TopBarSurface(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(if (expanded) 5 else 20)
                )
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                content()
            }
        }
    }
}

@Preview
@Composable
fun TopNavBarPreview() {
    TopNavBar(shouldShow = { true })
}
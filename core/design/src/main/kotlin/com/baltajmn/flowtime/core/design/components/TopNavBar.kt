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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.service.PlayerState
import com.baltajmn.flowtime.core.design.service.PlayerType
import com.baltajmn.flowtime.core.design.service.PlayerType.BROWN
import com.baltajmn.flowtime.core.design.service.PlayerType.PINK
import com.baltajmn.flowtime.core.design.service.PlayerType.WHITE
import com.baltajmn.flowtime.core.design.service.SoundViewModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

// Constants for better maintainability and performance
private object TopNavBarConstants {
    const val ANIMATION_DURATION_MS = 400
    const val INITIAL_DELAY_MS = 200L
    const val ROTATION_EXPANDED = 180f
    const val ROTATION_COLLAPSED = 0f
    const val SLIDE_OFFSET = 1000
    const val CORNER_RADIUS_EXPANDED = 5
    const val CORNER_RADIUS_COLLAPSED = 20
    const val ELEVATION = 5

    object Padding {
        val TOP = 40.dp
        val START = 24.dp
        val END = 24.dp
        val BOTTOM = 96.dp
        val CONTENT = 10.dp
        val SLIDER = 10.dp
        val SPACER = 10.dp
    }

    object Sizes {
        val EQUALIZER = 30.dp
        val SPACER = 10.dp
    }
}

@Composable
fun TopNavBar(
    modifier: Modifier = Modifier,
    viewModel: SoundViewModel = koinViewModel(),
    shouldShow: () -> Boolean
) {
    // State management with performance optimizations
    var expanded by rememberSaveable { mutableStateOf(false) }
    var firstVisibility by rememberSaveable { mutableStateOf(false) }

    // Collect state efficiently to avoid unnecessary recompositions
    val soundState by viewModel.uiState.collectAsState()

    // Optimized animation state
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) TopNavBarConstants.ROTATION_EXPANDED else TopNavBarConstants.ROTATION_COLLAPSED,
        animationSpec = tween(
            durationMillis = TopNavBarConstants.ANIMATION_DURATION_MS,
            easing = LinearEasing
        ),
        label = "rotation_animation"
    )

    // Lifecycle handling with proper cleanup
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pauseAllPlayers()
                }

                Lifecycle.Event.ON_RESUME -> {
                    viewModel.resumePlayingPlayers()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Cleanup when the effect leaves the composition
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial visibility delay
    LaunchedEffect(Unit) {
        delay(TopNavBarConstants.INITIAL_DELAY_MS)
        firstVisibility = true
    }

    // Optimized AnimatedVisibility with proper semantics
    AnimatedVisibility(
        modifier = modifier
            .navigationBarsPadding()
            .padding(
                top = TopNavBarConstants.Padding.TOP,
                start = TopNavBarConstants.Padding.START,
                end = TopNavBarConstants.Padding.END,
                bottom = TopNavBarConstants.Padding.BOTTOM
            )
            .semantics {
                contentDescription = "Sound control panel"
            },
        visible = shouldShow() && firstVisibility,
        enter = slideInHorizontally(
            initialOffsetX = { TopNavBarConstants.SLIDE_OFFSET },
            animationSpec = tween(
                durationMillis = TopNavBarConstants.ANIMATION_DURATION_MS,
                easing = LinearEasing
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { TopNavBarConstants.SLIDE_OFFSET },
            animationSpec = tween(
                durationMillis = TopNavBarConstants.ANIMATION_DURATION_MS,
                easing = LinearEasing
            )
        )
    ) {
        TopBarSurface(
            modifier = modifier
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = TopNavBarConstants.ANIMATION_DURATION_MS,
                        easing = LinearEasing
                    )
                )
                .wrapContentWidth(),
            expanded = expanded
        ) {
            CurrentlyPlaying(
                onExpandedClick = { expanded = !expanded },
                rotationState = rotationState,
                hasActivePlayers = soundState.soundMap.values.any { it.isPlaying }
            )

            if (expanded) {
                ExpandedContent(
                    items = soundState.soundMap,
                    onPlayClicked = { playerType, playing ->
                        viewModel.controlSounds(playerType = playerType, playing = playing)
                    },
                    onVolumeChanged = { itemPlayer, volume ->
                        viewModel.setVolume(type = itemPlayer, volume = volume)
                    }
                )
            }
        }
    }
}

@Composable
fun CurrentlyPlaying(
    onExpandedClick: () -> Unit,
    rotationState: Float,
    hasActivePlayers: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = if (hasActivePlayers) {
                "Sound controls - Currently playing audio"
            } else {
                "Sound controls - No audio playing"
            }
        }
    ) {
        LottieImage(
            modifier = Modifier.size(TopNavBarConstants.Sizes.EQUALIZER),
            animation = R.raw.equalizer,
            tintColor = if (hasActivePlayers) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )

        IconButton(
            modifier = Modifier
                .rotate(rotationState),
            onClick = onExpandedClick
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        }
    }
}

@Composable
fun ExpandedContent(
    items: Map<PlayerType, PlayerState>,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = "Sound mixer controls"
            }
    ) {
        PlayerType.entries.forEach { playerType ->
            val playerState = items[playerType]
            if (playerState != null) {
                SliderItem(
                    type = playerType,
                    playerState = playerState,
                    onPlayClicked = onPlayClicked,
                    onVolumeChanged = onVolumeChanged
                )
            }
        }
    }
}

@Composable
fun SliderItem(
    type: PlayerType,
    playerState: PlayerState,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit
) {
    val isPlaying = playerState.isPlaying
    val volume = playerState.volume

    val iconTint = remember(type) {
        when (type) {
            BROWN, PINK, WHITE -> Color.Unspecified
            else -> null
        }
    }

    val soundName = remember(type) {
        type.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TopNavBarConstants.Padding.SPACER)
    ) {
        Icon(
            painter = painterResource(type.icon),
            contentDescription = "$soundName sound",
            tint = iconTint ?: MaterialTheme.colorScheme.secondary,
            modifier = Modifier.semantics {
                contentDescription = "$soundName sound icon"
            }
        )

        Slider(
            modifier = Modifier
                .padding(horizontal = TopNavBarConstants.Padding.SLIDER)
                .weight(1f),
            value = volume,
            onValueChange = { newVolume ->
                onVolumeChanged(type, newVolume)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            ),
            valueRange = 0f..1f
        )

        IconButton(
            onClick = {
                onPlayClicked(type, !isPlaying)
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = null // Handled by parent semantics
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
    val cornerRadius = remember(expanded) {
        if (expanded) TopNavBarConstants.CORNER_RADIUS_EXPANDED
        else TopNavBarConstants.CORNER_RADIUS_COLLAPSED
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = modifier
                .shadow(
                    elevation = TopNavBarConstants.ELEVATION.dp,
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
                .align(Alignment.Center),
            shape = RoundedCornerShape(cornerRadius.dp)
        ) {
            Column(
                modifier = Modifier.padding(TopNavBarConstants.Padding.CONTENT),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

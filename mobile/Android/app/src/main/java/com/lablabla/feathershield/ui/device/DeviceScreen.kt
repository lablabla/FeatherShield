package com.lablabla.feathershield.ui.device

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    navController: NavController,
    viewModel: DeviceViewModel = hiltViewModel(),
) {
    val device by viewModel.device.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nestbox: ${device?.id ?: "Loading..."}") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (device == null) {
                CircularProgressIndicator()
            } else {
                Text(text = "Battery: ${device?.batteryLevel}%")
                Spacer(modifier = Modifier.height(16.dp))
                RtspPlayer(device!!.liveStreamUrl)
//                AsyncImage(
//                    model = ImageRequest.Builder(LocalContext.current)
//                        .data(device?.lastImageUrl)
//                        .crossfade(true)
//                        .build(),
////                    contentScale = ContentScale.FillBounds,
//                    contentDescription = "Latest image from device",
//                    onError = { throwable ->
//                        // Handle the error, e.g., log it or show a message
//                        Log.e("CoilError", "Image loading failed: ${throwable.result}")
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth())
                {
                    Button(
                        onClick = {
                            viewModel.startStream();
                        }
                    ) {
                        Text("Start Stream")
                    }
                    Button(
                        onClick = {
                            viewModel.stopStream();
                        }
                    ) {
                        Text("Stop Stream")
                    }

                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {}
                ) {
                    Text("Spray")
                }

            }
        }
    }
}

@Composable
fun RtspPlayer(rtspUri: String) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        player.setMediaItem(MediaItem.fromUri(rtspUri))
        player.prepare()
        player.playWhenReady = true

        onDispose {
            player.release()
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize(0.75f)
    )
}

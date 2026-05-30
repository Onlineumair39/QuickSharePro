package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveTransferState
import com.example.ui.ShareViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: ShareViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val activeTransfer by viewModel.activeTransfer.collectAsStateWithLifecycle()

    // Control variable to simulate request prompt
    var showRequestPrompt by remember { mutableStateOf(false) }

    // Handlers to trigger simulation automatically on launch
    LaunchedEffect(Unit) {
        if (!activeTransfer.isTransferring) {
            delay(2200) // Delay before simulation request rings
            showRequestPrompt = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (activeTransfer.isTransferring) "Active Transfer Receiver" else "Receive Files",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeTransfer.isTransferring) {
                                viewModel.cancelTransfer()
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("receive_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back button"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTransfer.isTransferring) {
                // Showing active receiver screen overlay
                ActiveTransferOverlay(
                    state = activeTransfer,
                    onCancel = { viewModel.cancelTransfer() },
                    onDone = {
                        viewModel.cancelTransfer()
                        onNavigateBack()
                    }
                )
            } else {
                // Advertising beacon scanning screen
                ReceiveAdvertisingPanel(
                    deviceName = deviceName,
                    onSimulateIncoming = {
                        showRequestPrompt = true
                    }
                )

                // Prompt modal for receiving a connection
                if (showRequestPrompt) {
                    AlertDialog(
                        onDismissRequest = { showRequestPrompt = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        title = {
                            Text(
                                "Incoming Connection",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Galaxy S24 Ultra",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "wants to share 3 files (total size 53.9 MB) with you. Accept this transfer?",
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showRequestPrompt = false
                                    viewModel.startReceivingFlow()
                                },
                                modifier = Modifier.testTag("accept_receive_request"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Accept")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showRequestPrompt = false },
                                modifier = Modifier.testTag("dismiss_receive_request")
                            ) {
                                Text("Decline", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiveAdvertisingPanel(
    deviceName: String,
    onSimulateIncoming: () -> Unit
) {
    // Elegant background pulsing wave animations
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Explanatory texts
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Advertising Visibility",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Searching for nearby sender signals offline",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f)
            )
        }

        // Radar beacon transmitter
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Under layers pulsing rings
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.18f),
                        CircleShape
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.6f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale((pulseScale + 0.3f).let { if (it > 1f) it - 1f else it })
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha * 0.12f),
                        CircleShape
                    )
            )

            // Core card container with radar dish logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Advertising device signature card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECEIVING AS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.tertiary, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Broadcasting Wi-Fi Hotspot details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Quick simulator option (Since we are on a single browser streaming emulator, we can tap to trigger a request)
        Button(
            onClick = onSimulateIncoming,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("simulate_incoming_request")
        ) {
            Icon(imageVector = Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Receive Request Immediately", fontWeight = FontWeight.Bold)
        }
    }
}

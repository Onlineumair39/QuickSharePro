package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveTransferState
import com.example.ui.FileSelectionItem
import com.example.ui.PeerDevice
import com.example.ui.ShareViewModel
import com.example.ui.utils.UiUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: ShareViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val activeTransfer by viewModel.activeTransfer.collectAsStateWithLifecycle()

    // 0 = Select Files, 1 = Discover Devices / Sending
    var sendPhaseState by remember { mutableStateOf(0) }

    // Re-route dynamically if an active transfer finishes or is active
    LaunchedEffect(activeTransfer.isTransferring) {
        if (activeTransfer.isTransferring) {
            sendPhaseState = 1 // locks onto transfer console
        }
    }

    // Android Storage File Picker Activity Launcher
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        uris?.forEach { uri ->
            val metadata = getFileInfo(context, uri)
            viewModel.addCustomPickedFile(
                name = metadata.name,
                size = metadata.size,
                type = metadata.type,
                uri = uri.toString()
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (activeTransfer.isTransferring) "Transfer Console" else if (sendPhaseState == 1) "Scan Devices" else "Select Files",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeTransfer.isTransferring) {
                                viewModel.cancelTransfer()
                            } else if (sendPhaseState == 1) {
                                viewModel.stopDiscovery()
                                sendPhaseState = 0
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back button"
                        )
                    }
                },
                actions = {
                    if (sendPhaseState == 0 && selectedFiles.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearSelections() },
                            modifier = Modifier.testTag("clear_selections")
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
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
            when {
                activeTransfer.isTransferring -> {
                    // Active Transfer UI screen (Progress meters, Cancel triggers)
                    ActiveTransferOverlay(
                        state = activeTransfer,
                        onCancel = { viewModel.cancelTransfer() },
                        onDone = {
                            viewModel.cancelTransfer()
                            onNavigateBack()
                        }
                    )
                }

                sendPhaseState == 1 -> {
                    // Peer Discovery UI radar screen
                    DiscoveryRadarPanel(
                        isDiscovering = isDiscovering,
                        peersList = discoveredPeers,
                        onPeerSelected = { peer ->
                            viewModel.startSendingFiles(peer)
                        },
                        onBackToSelector = {
                            viewModel.stopDiscovery()
                            sendPhaseState = 0
                        }
                    )
                }

                else -> {
                    // Selection view (M3 Category tabs & mock previews + external phone picker)
                    FileSelectorPanel(
                        viewModel = viewModel,
                        selectedFiles = selectedFiles,
                        onPickExternalFiles = {
                            pickFileLauncher.launch("*/*")
                        },
                        onProceedToScan = {
                            sendPhaseState = 1
                            viewModel.startDiscovery()
                        }
                    )
                }
            }
        }
    }
}

// Subcomponent: File selection tabs screen
@Composable
fun FileSelectorPanel(
    viewModel: ShareViewModel,
    selectedFiles: List<FileSelectionItem>,
    onPickExternalFiles: () -> Unit,
    onProceedToScan: () -> Unit
) {
    var activeCategoryIndex by remember { mutableStateOf(0) }
    val tabCategories = listOf("Images", "Videos", "Audio", "Documents", "ZIP/APK")

    // Filter available mock list by active tab category
    val filteredMocks = remember(activeCategoryIndex) {
        viewModel.mockAvailableFiles.filter {
            when (activeCategoryIndex) {
                0 -> it.type == "IMAGE"
                1 -> it.type == "VIDEO"
                2 -> it.type == "AUDIO"
                3 -> it.type == "DOCUMENT"
                4 -> it.type == "ZIP" || it.type == "APK"
                else -> true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Styled Categories Tabs
            ScrollableTabRow(
                selectedTabIndex = activeCategoryIndex,
                edgePadding = 16.dp,
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabCategories.forEachIndexed { idx, category ->
                    Tab(
                        selected = activeCategoryIndex == idx,
                        onClick = { activeCategoryIndex = idx },
                        text = { Text(category, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Selected indicator summary bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preset Local Demos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )

                // Dynamic picker option
                Button(
                    onClick = onPickExternalFiles,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("file_picker_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pick from Storage", style = MaterialTheme.typography.labelSmall)
                }
            }

            // List files of active category
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMocks, key = { it.name }) { item ->
                    val isSelected = selectedFiles.any { it.name == item.name }
                    FileItemCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = { viewModel.toggleFileSelection(item) }
                    )
                }
            }
        }

        // Floating Bottom Confirmation overlay
        AnimatedVisibility(
            visible = selectedFiles.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val totalSizeSum = selectedFiles.sumOf { it.size }
                        Text(
                            text = "${selectedFiles.size} ${if (selectedFiles.size == 1) "file" else "files"} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = UiUtils.formatBytes(totalSizeSum),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Button(
                        onClick = onProceedToScan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("send_selected_confirm")
                    ) {
                        Text("Proceed to Scan", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.CompassCalibration, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// Subcomponent: File list card
@Composable
fun FileItemCard(
    item: FileSelectionItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = UiUtils.getIconForFileType(item.type),
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = UiUtils.formatBytes(item.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Selection State Indicator",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Subcomponent: Peer Discovery scanning screen
@Composable
fun DiscoveryRadarPanel(
    isDiscovering: Boolean,
    peersList: List<PeerDevice>,
    onPeerSelected: (PeerDevice) -> Unit,
    onBackToSelector: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High fidelity Radar scanning animation
        val infiniteTransition = rememberInfiniteTransition(label = "radar")
        val scanAnimScale by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radar_scale"
        )
        val scanAnimAlpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radar_alpha"
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isDiscovering) {
                // Expanding overlapping pulsing radar rings
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scanAnimScale)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = scanAnimAlpha * 0.25f),
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = scanAnimAlpha), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale((scanAnimScale + 0.5f).let { if (it > 1f) it - 1f else it })
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = scanAnimAlpha * 0.15f),
                            shape = CircleShape
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = scanAnimAlpha * 0.5f), CircleShape)
                )
            }

            // Radar central scanner dish core
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = "Radar scanner antenna",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scanning Nearby Sharing Devices...",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Make sure receiving device has 'Receive Screen' active.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Discovered items list
        Text(
            text = "Available Peers (${peersList.size})",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        if (peersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Searching for clients over Wi-Fi direct...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(peersList, key = { it.id }) { peer ->
                    PeerCard(peer = peer, onClick = { onPeerSelected(peer) })
                }
            }
        }
    }
}

// Subcomponent: Discovered peer item
@Composable
fun PeerCard(
    peer: PeerDevice,
    onClick: () -> Unit
) {
    val isConnecting = peer.status == "Connecting"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "HW Address: ${peer.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Connect", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// Subcomponent: Live Active progress tracker overlay
@Composable
fun ActiveTransferOverlay(
    state: ActiveTransferState = ActiveTransferState(),
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val isDone = state.transferStatus == "COMPLETED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Animation Header state
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isDone) "Transfer Succeeded" else "Sharing Core Processing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isDone) "All files have been successfully structured" else "To peer device: ${state.peerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }

        // Circular Speed & Percentage display meter
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { state.progressPercent },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 14.dp,
                color = if (isDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Text(
                        text = "${(state.progressPercent * 100).toInt()}%",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (state.transferStatus == "CONNECTING") "Handshaking" else UiUtils.formatSpeed(state.speedBytesPerSec),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Live statistics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transfer Volume:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = "${UiUtils.formatBytes(state.bytesTransferred)} of ${UiUtils.formatBytes(state.totalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Approx. Time Remaining:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = if (isDone) "None" else "${state.remainingSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active Item:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        text = if (isDone) "Completed" else state.currentFileName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }
            }
        }

        // Control Button
        if (isDone) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("transfer_complete_done"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Finish & Return", fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("transfer_cancel_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abort Transfer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helper model & metadata extractor function of real selected files
data class FileMeta(val name: String, val size: Long, val type: String)

fun getFileInfo(context: Context, uri: Uri): FileMeta {
    var name = "unknown_file"
    var size = 0L
    var type = "DOCUMENT"

    val mimeType = context.contentResolver.getType(uri) ?: ""
    type = when {
        mimeType.startsWith("image/") -> "IMAGE"
        mimeType.startsWith("video/") -> "VIDEO"
        mimeType.startsWith("audio/") -> "AUDIO"
        mimeType.endsWith("zip") || mimeType.contains("compressed") -> "ZIP"
        mimeType.contains("android.package-archive") -> "APK"
        else -> "DOCUMENT"
    }

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex)
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }

    if (name.endsWith(".zip", true)) type = "ZIP"
    if (name.endsWith(".apk", true)) type = "APK"

    return FileMeta(name = name, size = size, type = type)
}

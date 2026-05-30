package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.QuickShareApp
import com.example.data.SettingsRepository
import com.example.data.TransferHistory
import com.example.data.TransferRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

data class FileSelectionItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val size: Long,
    val type: String, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "ZIP", "APK"
    val uriString: String? = null
)

data class PeerDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val status: String = "Available" // "Available", "Connecting", "Connected", "Failed"
)

data class ActiveTransferState(
    val isTransferring: Boolean = false,
    val direction: String = "SEND", // "SEND", "RECEIVE"
    val currentFileName: String = "",
    val totalSize: Long = 0,
    val bytesTransferred: Long = 0,
    val speedBytesPerSec: Long = 0,
    val remainingSeconds: Long = 0,
    val progressPercent: Float = 0f,
    val peerName: String = "",
    val files: List<FileSelectionItem> = emptyList(),
    val currentFileIndex: Int = 0,
    val transferStatus: String = "IDLE" // "IDLE", "CONNECTING", "SENDING", "RECEIVING", "COMPLETED", "FAILED"
)

class ShareViewModel(
    application: Application,
    private val transferRepository: TransferRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    // Settings flows
    val deviceName: StateFlow<String> = settingsRepository.deviceName
    val downloadLocation: StateFlow<String> = settingsRepository.downloadLocation
    val darkThemeMode: StateFlow<Int> = settingsRepository.darkThemeMode

    // Database flow
    val historyState: StateFlow<List<TransferHistory>> = transferRepository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Interactive States
    private val _selectedFiles = MutableStateFlow<List<FileSelectionItem>>(emptyList())
    val selectedFiles: StateFlow<List<FileSelectionItem>> = _selectedFiles.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _activeTransfer = MutableStateFlow(ActiveTransferState())
    val activeTransfer: StateFlow<ActiveTransferState> = _activeTransfer.asStateFlow()

    private var transferJob: Job? = null
    private var discoveryJob: Job? = null

    // Predefined Mock files for simulated selection in Send screen (to give a high-fidelity visual experience)
    val mockAvailableFiles = listOf(
        FileSelectionItem(name = "IMG_4829_Nature.jpg", size = 4210000, type = "IMAGE"),
        FileSelectionItem(name = "IMG_9103_Profile.png", size = 1850000, type = "IMAGE"),
        FileSelectionItem(name = "VID_Trip_To_Swiss.mp4", size = 84500000, type = "VIDEO"),
        FileSelectionItem(name = "VID_Presentation_v2.mp4", size = 41200000, type = "VIDEO"),
        FileSelectionItem(name = "AudioBook_Sapiens.mp3", size = 153000000, type = "AUDIO"),
        FileSelectionItem(name = "Song_Midnight_City.flac", size = 28400000, type = "AUDIO"),
        FileSelectionItem(name = "Resume_Software_Engineer.pdf", size = 320000, type = "DOCUMENT"),
        FileSelectionItem(name = "Project_Pitch_Deck.pptx", size = 12500000, type = "DOCUMENT"),
        FileSelectionItem(name = "Backup_Resources.zip", size = 245000000, type = "ZIP"),
        FileSelectionItem(name = "Flutter_Release_v1.apk", size = 38200000, type = "APK")
    )

    // Simulated peer names for WifiP2p scanning simulator
    private val peerNames = listOf("Galaxy S24 Ultra", "Pixel 9 Pro XL", "OnePlus 12", "Redmi Note 13", "iPad Air", "Xperia 1 V", "Infinix Zero 30")

    init {
        // Start discovering peers automatically for nice animations, or let the screen toggle it
    }

    // Toggle discovery mock
    fun startDiscovery() {
        if (_isDiscovering.value) return
        _isDiscovering.value = true
        _discoveredPeers.value = emptyList()

        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            // Gradually find local devices
            var index = 0
            while (_isDiscovering.value && index < peerNames.size) {
                delay(1200)
                if (!_isDiscovering.value) break
                val name = peerNames[index]
                val mac = "3A:B8:C9:F1:${Random.nextInt(10, 99)}:${Random.nextInt(10, 99)}"
                val newPeer = PeerDevice(name = name, address = mac)
                _discoveredPeers.value = _discoveredPeers.value + newPeer
                index++
            }
        }
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        discoveryJob?.cancel()
    }

    // Selected file utility methods
    fun toggleFileSelection(item: FileSelectionItem) {
        val current = _selectedFiles.value
        if (current.any { it.name == item.name }) {
            _selectedFiles.value = current.filterNot { it.name == item.name }
        } else {
            _selectedFiles.value = current + item
        }
    }

    fun addCustomPickedFile(name: String, size: Long, type: String, uri: String) {
        val newItem = FileSelectionItem(name = name, size = size, type = type, uriString = uri)
        _selectedFiles.value = _selectedFiles.value + newItem
    }

    fun clearSelections() {
        _selectedFiles.value = emptyList()
    }

    // Settings actions
    fun renameDevice(newNm: String) {
        settingsRepository.updateDeviceName(newNm)
    }

    fun changeDownloadLocation(path: String) {
        settingsRepository.updateDownloadLocation(path)
    }

    fun toggleThemeSelection(mode: Int) {
        settingsRepository.updateThemeMode(mode)
    }

    // Clear history items
    fun clearAllHistory() {
        viewModelScope.launch {
            transferRepository.clearAll()
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            transferRepository.deleteById(id)
        }
    }

    // Simulated Wi-Fi Direct file transfer engine
    fun startSendingFiles(peer: PeerDevice) {
        if (_selectedFiles.value.isEmpty()) return
        stopDiscovery()

        // Update selected peer status to connecting
        _discoveredPeers.value = _discoveredPeers.value.map {
            if (it.id == peer.id) it.copy(status = "Connecting") else it
        }

        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            // Connection phase (simulating Wi-Fi direct connection handshakes)
            _activeTransfer.value = ActiveTransferState(
                isTransferring = true,
                direction = "SEND",
                peerName = peer.name,
                files = _selectedFiles.value,
                transferStatus = "CONNECTING"
            )

            delay(2000) // Simulated Wi-Fi Direct link negotiation

            // Connected
            _discoveredPeers.value = _discoveredPeers.value.map {
                if (it.id == peer.id) it.copy(status = "Connected") else it
            }

            val filesToSend = _selectedFiles.value
            val totalBytes = filesToSend.sumOf { it.size }
            var totalBytesSent = 0L

            for (index in filesToSend.indices) {
                val file = filesToSend[index]
                _activeTransfer.value = _activeTransfer.value.copy(
                    currentFileName = file.name,
                    currentFileIndex = index,
                    transferStatus = "SENDING"
                )

                var fileBytesSent = 0L
                val fileSize = file.size

                while (fileBytesSent < fileSize) {
                    if (!_activeTransfer.value.isTransferring) {
                        // Transfer cancelled
                        dbRecordTransfer(file, peer.name, "SEND", "FAILED", 0)
                        return@launch
                    }

                    // Dynamically vary speeds for realism (between 5MB/s and 25MB/s)
                    val currentSpeedSec = Random.nextLong(6_000_000, 26_000_000)
                    delay(500) // update twice per second
                    val chunk = currentSpeedSec / 2

                    fileBytesSent = (fileBytesSent + chunk).coerceAtMost(fileSize)
                    totalBytesSent = filesToSend.take(index).sumOf { it.size } + fileBytesSent

                    val percent = if (totalBytes > 0) totalBytesSent.toFloat() / totalBytes else 0f
                    val secondsRemaining = if (currentSpeedSec > 0) max(1, (totalBytes - totalBytesSent) / currentSpeedSec) else 99L

                    _activeTransfer.value = _activeTransfer.value.copy(
                        bytesTransferred = totalBytesSent,
                        totalSize = totalBytes,
                        speedBytesPerSec = currentSpeedSec,
                        remainingSeconds = secondsRemaining,
                        progressPercent = percent
                    )
                }

                // File completed transfer safely
                dbRecordTransfer(file, peer.name, "SEND", "COMPLETED", _activeTransfer.value.speedBytesPerSec)
            }

            // All files finished
            _activeTransfer.value = _activeTransfer.value.copy(
                progressPercent = 1.0f,
                transferStatus = "COMPLETED"
            )

            // Reset peer status
            _discoveredPeers.value = _discoveredPeers.value.map {
                if (it.id == peer.id) it.copy(status = "Available") else it
            }

            delay(2000) // Stay on completed status screen briefly
            _activeTransfer.value = ActiveTransferState() // Reset
            clearSelections()
        }
    }

    // Receive handler mock
    fun startReceivingFlow() {
        transferJob?.cancel()
        transferJob = viewModelScope.launch {
            // Simulated connection incoming from standard peer
            _activeTransfer.value = ActiveTransferState(
                isTransferring = true,
                direction = "RECEIVE",
                peerName = "Galaxy S24 Ultra",
                transferStatus = "CONNECTING"
            )

            delay(2500) // Simulated peer handshake request

            // Simulated received file list
            val incomingFiles = listOf(
                FileSelectionItem(name = "Party_Photo_HQ.png", size = 6800000, type = "IMAGE"),
                FileSelectionItem(name = "Shared_Document_Draft.docx", size = 2100000, type = "DOCUMENT"),
                FileSelectionItem(name = "New_Game_Asset.apk", size = 45000000, type = "APK")
            )

            _activeTransfer.value = _activeTransfer.value.copy(
                files = incomingFiles,
                transferStatus = "RECEIVING",
                totalSize = incomingFiles.sumOf { it.size }
            )

            val totalBytes = incomingFiles.sumOf { it.size }
            var totalBytesReceived = 0L

            for (index in incomingFiles.indices) {
                val file = incomingFiles[index]
                _activeTransfer.value = _activeTransfer.value.copy(
                    currentFileName = file.name,
                    currentFileIndex = index
                )

                var fileBytesRec = 0L
                val fileSize = file.size

                while (fileBytesRec < fileSize) {
                    if (!_activeTransfer.value.isTransferring) {
                        dbRecordTransfer(file, "Galaxy S24 Ultra", "RECEIVE", "FAILED", 0)
                        return@launch
                    }

                    val currentSpeedSec = Random.nextLong(8_000_000, 28_000_000)
                    delay(500)
                    val chunk = currentSpeedSec / 2

                    fileBytesRec = (fileBytesRec + chunk).coerceAtMost(fileSize)
                    totalBytesReceived = incomingFiles.take(index).sumOf { it.size } + fileBytesRec

                    val percent = if (totalBytes > 0) totalBytesReceived.toFloat() / totalBytes else 0f
                    val secondsRemaining = if (currentSpeedSec > 0) max(1, (totalBytes - totalBytesReceived) / currentSpeedSec) else 99L

                    _activeTransfer.value = _activeTransfer.value.copy(
                        bytesTransferred = totalBytesReceived,
                        speedBytesPerSec = currentSpeedSec,
                        remainingSeconds = secondsRemaining,
                        progressPercent = percent
                    )
                }

                dbRecordTransfer(file, "Galaxy S24 Ultra", "RECEIVE", "COMPLETED", _activeTransfer.value.speedBytesPerSec)
            }

            _activeTransfer.value = _activeTransfer.value.copy(
                progressPercent = 1.0f,
                transferStatus = "COMPLETED"
            )

            delay(2000)
            _activeTransfer.value = ActiveTransferState()
        }
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        _activeTransfer.value = ActiveTransferState()
    }

    private suspend fun dbRecordTransfer(
        file: FileSelectionItem,
        peer: String,
        direction: String,
        status: String,
        speed: Long
    ) {
        val historyEntry = TransferHistory(
            fileName = file.name,
            fileSize = file.size,
            fileType = file.type,
            direction = direction,
            peerName = peer,
            status = status,
            transferSpeed = speed
        )
        transferRepository.insert(historyEntry)
    }
}

class ShareViewModelFactory(
    private val application: Application,
    private val transferRepository: TransferRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShareViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShareViewModel(application, transferRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

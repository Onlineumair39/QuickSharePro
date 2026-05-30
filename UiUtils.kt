package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

object UiUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun getIconForFileType(type: String): ImageVector {
        return when (type.uppercase(Locale.getDefault())) {
            "IMAGE" -> Icons.Default.Image
            "VIDEO" -> Icons.Default.VideoFile
            "AUDIO" -> Icons.Default.AudioFile
            "DOCUMENT" -> Icons.Default.Description
            "ZIP" -> Icons.Default.FolderZip
            "APK" -> Icons.Default.Android
            else -> Icons.Default.InsertDriveFile
        }
    }
}

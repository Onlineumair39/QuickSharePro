package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val fileSize: Long,
    val fileType: String, // "IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "ZIP", "APK"
    val direction: String, // "SEND", "RECEIVE"
    val peerName: String,
    val status: String, // "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val transferSpeed: Long // Average speed in bytes per second
)

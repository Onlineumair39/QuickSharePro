package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransferRepository(private val dao: TransferHistoryDao) {
    val allHistory: Flow<List<TransferHistory>> = dao.getAllHistory()

    suspend fun insert(history: TransferHistory) = withContext(Dispatchers.IO) {
        dao.insertHistory(history)
    }

    suspend fun deleteById(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAllHistory()
    }
}

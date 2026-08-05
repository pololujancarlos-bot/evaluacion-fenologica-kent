package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.EvaluationDao
import com.example.data.local.EvaluationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EvaluationRepository(
    private val dao: EvaluationDao,
    private val context: Context
) {

    val allEvaluations: Flow<List<EvaluationEntity>> = dao.getAllEvaluations()
    val pendingCount: Flow<Int> = dao.getPendingCountFlow()
    val totalCount: Flow<Int> = dao.getTotalCountFlow()

    suspend fun getEvaluationById(id: String): EvaluationEntity? = withContext(Dispatchers.IO) {
        dao.getEvaluationById(id)
    }

    suspend fun saveEvaluation(evaluation: EvaluationEntity) = withContext(Dispatchers.IO) {
        dao.insertEvaluation(evaluation)
    }

    suspend fun deleteEvaluation(id: String) = withContext(Dispatchers.IO) {
        dao.deleteEvaluationById(id)
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun syncPendingRecords(): Result<Int> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(Exception("Sin conexión a internet. Los datos están guardados localmente en SQLite."))
        }

        val pendingList = dao.getPendingSyncEvaluations()
        if (pendingList.isEmpty()) {
            return@withContext Result.success(0)
        }

        var syncedCount = 0
        val now = System.currentTimeMillis()

        for (item in pendingList) {
            // Simulate cloud backend synchronization latency per payload
            delay(300)
            val generatedRemoteId = "REM-KENT-${item.id.take(8).uppercase()}"
            dao.markAsSynced(item.id, now, generatedRemoteId)
            syncedCount++
        }

        Result.success(syncedCount)
    }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {

    @Query("SELECT * FROM evaluaciones_kent ORDER BY timestamp DESC")
    fun getAllEvaluations(): Flow<List<EvaluationEntity>>

    @Query("SELECT * FROM evaluaciones_kent WHERE id = :id")
    suspend fun getEvaluationById(id: String): EvaluationEntity?

    @Query("SELECT * FROM evaluaciones_kent WHERE syncStatus = 'PENDIENTE'")
    suspend fun getPendingSyncEvaluations(): List<EvaluationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(evaluation: EvaluationEntity)

    @Update
    suspend fun updateEvaluation(evaluation: EvaluationEntity)

    @Query("DELETE FROM evaluaciones_kent WHERE id = :id")
    suspend fun deleteEvaluationById(id: String)

    @Query("UPDATE evaluaciones_kent SET syncStatus = 'SINCRONIZADO', lastSyncTimestamp = :syncTime, remoteId = :remoteId WHERE id = :id")
    suspend fun markAsSynced(id: String, syncTime: Long, remoteId: String)

    @Query("SELECT COUNT(*) FROM evaluaciones_kent WHERE syncStatus = 'PENDIENTE'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM evaluaciones_kent")
    fun getTotalCountFlow(): Flow<Int>
}

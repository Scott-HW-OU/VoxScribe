package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE id = :id LIMIT 1")
    fun observeRecordingById(id: Long): Flow<CallRecordingEntity?>

    @Query("SELECT * FROM call_recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): CallRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)

    @Query("DELETE FROM call_recordings WHERE contactName LIKE '%Elena Vance%' OR phoneNumber LIKE '%890-4312%'")
    suspend fun deleteLegacyExampleRecordings()

    @Query("SELECT * FROM call_recordings WHERE cloudSyncStatus != 'SYNCED' ORDER BY timestamp DESC")
    suspend fun getUnsyncedRecordings(): List<CallRecordingEntity>
}

package com.arnix.codepad.studio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeProjectDao {
    @Query("SELECT * FROM code_projects WHERE id = 1 LIMIT 1")
    fun observeActiveProject(): Flow<CodeProjectEntity?>

    @Query("SELECT * FROM code_projects WHERE id = 1 LIMIT 1")
    suspend fun getActiveProjectOnce(): CodeProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: CodeProjectEntity)

    @Query("SELECT * FROM code_revisions ORDER BY timestamp DESC")
    fun observeSnapshots(): Flow<List<CodeRevisionSnapshot>>

    @Query("SELECT * FROM code_revisions ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentSnapshots(): List<CodeRevisionSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: CodeRevisionSnapshot): Long

    @Query("DELETE FROM code_revisions WHERE id = :id")
    suspend fun deleteSnapshot(id: Long)

    @Query("DELETE FROM code_revisions")
    suspend fun clearAllSnapshots()
}

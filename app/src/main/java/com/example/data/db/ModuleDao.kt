package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on imported shell modules.
 */
@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules ORDER BY importDate DESC")
    fun getAllModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE id = :id LIMIT 1")
    suspend fun getModuleById(id: Long): ModuleEntity?

    @Query("SELECT * FROM modules WHERE uri = :uri LIMIT 1")
    suspend fun getModuleByUri(uri: String): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModuleEntity): Long

    @Update
    suspend fun updateModule(module: ModuleEntity)

    @Delete
    suspend fun deleteModule(module: ModuleEntity)

    @Query("DELETE FROM modules WHERE id = :id")
    suspend fun deleteModuleById(id: Long)

    @Query("UPDATE modules SET lastRunDate = :lastRunDate, lastExitCode = :exitCode WHERE id = :id")
    suspend fun updateLastRun(id: Long, lastRunDate: Long, exitCode: Int)
}

package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing an imported shell script / module.
 * Persists module metadata and URI across application restarts.
 */
@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val uri: String,
    val size: Long = 0L,
    val importDate: Long = System.currentTimeMillis(),
    val validationStatus: String = "VALID", // VALID, WARNING, EMPTY, ERROR
    val validationMessage: String = "",
    val previewContent: String = "",
    val lastRunDate: Long? = null,
    val lastExitCode: Int? = null
)

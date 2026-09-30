package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.db.ModuleDao
import com.example.data.db.ModuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class ValidationResult(
    val status: String, // "VALID", "WARNING", "EMPTY", "ERROR"
    val message: String,
    val preview: String,
    val fullContent: String
)

class ModuleRepository(
    private val moduleDao: ModuleDao,
    private val context: Context
) {
    val allModules: Flow<List<ModuleEntity>> = moduleDao.getAllModules()

    suspend fun getModuleById(id: Long): ModuleEntity? = withContext(Dispatchers.IO) {
        moduleDao.getModuleById(id)
    }

    suspend fun insertModule(module: ModuleEntity): Long = withContext(Dispatchers.IO) {
        moduleDao.insertModule(module)
    }

    suspend fun updateModule(module: ModuleEntity) = withContext(Dispatchers.IO) {
        moduleDao.updateModule(module)
    }

    suspend fun deleteModule(module: ModuleEntity) = withContext(Dispatchers.IO) {
        moduleDao.deleteModule(module)
    }

    suspend fun deleteModuleById(id: Long) = withContext(Dispatchers.IO) {
        moduleDao.deleteModuleById(id)
    }

    suspend fun updateLastRun(id: Long, exitCode: Int) = withContext(Dispatchers.IO) {
        moduleDao.updateLastRun(id, System.currentTimeMillis(), exitCode)
    }

    /**
     * Safely takes persistable URI permission so the file can be read across app reboots.
     */
    fun takePersistableUriPermission(uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: SecurityException) {
            // Some file providers or virtual folders don't support persistable permissions
        } catch (_: Exception) {
            // Ignore other exceptions during permission grant
        }
    }

    /**
     * Query display name and size from SAF Uri.
     */
    fun getUriMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "unknown_script.sh"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to defaults
        }

        return Pair(name, size)
    }

    /**
     * Reads and validates the shell script content from the URI.
     */
    suspend fun readAndValidateScript(uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext ValidationResult(
                    status = "ERROR",
                    message = "Cannot open input stream for file URI. File may have been moved or deleted.",
                    preview = "",
                    fullContent = ""
                )

            val reader = BufferedReader(InputStreamReader(inputStream))
            val fullContent = StringBuilder()
            val previewBuilder = StringBuilder()
            var lineCount = 0
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                fullContent.append(line).append("\n")
                if (lineCount < 20) {
                    previewBuilder.append(line).append("\n")
                    lineCount++
                }
            }
            reader.close()

            val scriptText = fullContent.toString().trim()
            if (scriptText.isEmpty()) {
                return@withContext ValidationResult(
                    status = "EMPTY",
                    message = "The selected file is empty. Nothing to execute.",
                    preview = "",
                    fullContent = ""
                )
            }

            // Inspect content for potentially hazardous or high-privilege operations
            val warnings = mutableListOf<String>()
            val lower = scriptText.lowercase()

            if (lower.contains("rm -rf /") || lower.contains("rm -rf /*")) {
                warnings.add("Destructive command detected: 'rm -rf /'")
            }
            if (lower.contains("reboot") || lower.contains("/system/bin/reboot")) {
                warnings.add("Device reboot command detected")
            }
            if (lower.contains("dd if=") || lower.contains("/dev/block/")) {
                warnings.add("Raw block device partition write detected (dd/block)")
            }
            if (lower.contains("mount -o remount,rw") || lower.contains("remount")) {
                warnings.add("System partition remount detected")
            }
            if (lower.contains("setenforce 0")) {
                warnings.add("SELinux permissive switch detected")
            }
            if (lower.contains("pm uninstall") || lower.contains("pm disable")) {
                warnings.add("Package manager uninstall/disable action detected")
            }

            val status = if (warnings.isNotEmpty()) "WARNING" else "VALID"
            val message = if (warnings.isNotEmpty()) {
                "High privilege warning:\n" + warnings.joinToString("\n• ", prefix = "• ")
            } else {
                "Valid shell script ready for execution."
            }

            ValidationResult(
                status = status,
                message = message,
                preview = previewBuilder.toString().trimEnd(),
                fullContent = scriptText
            )
        } catch (e: Exception) {
            ValidationResult(
                status = "ERROR",
                message = "Failed to read file: ${e.localizedMessage ?: e.message}",
                preview = "",
                fullContent = ""
            )
        }
    }
}

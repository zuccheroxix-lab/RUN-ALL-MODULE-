package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class ExecutionStatus {
    IDLE,
    CHECKING_SHIZUKU,
    READY,
    RUNNING,
    SUCCESS,
    FAILED,
    STOPPED
}

enum class LogType {
    STDOUT,
    STDERR,
    SYSTEM,
    ERROR
}

data class ShellLogEntry(
    val id: Long = System.nanoTime(),
    val text: String,
    val type: LogType,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExecutionState(
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val currentCommandTitle: String = "",
    val exitCode: Int? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val logs: List<ShellLogEntry> = emptyList(),
    val autoScroll: Boolean = true
)

class ShellExecutor(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(ExecutionState())
    val state: StateFlow<ExecutionState> = _state.asStateFlow()

    private var activeProcess: Process? = null
    private var executionJob: Job? = null

    /**
     * Determines the most appropriate shell binary available on the device.
     */
    fun detectShell(): String {
        return when {
            File("/system/bin/sh").exists() -> "/system/bin/sh"
            File("/bin/sh").exists() -> "/bin/sh"
            File("/system/bin/bash").exists() -> "/system/bin/bash"
            else -> "/system/bin/sh"
        }
    }

    /**
     * Executes a command or script content using the Shizuku privileged shell process.
     */
    fun execute(
        commandTitle: String,
        scriptOrCommand: String,
        onComplete: ((exitCode: Int) -> Unit)? = null
    ) {
        // Stop any running execution first
        stopExecution()

        executionJob = scope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                status = ExecutionStatus.CHECKING_SHIZUKU,
                currentCommandTitle = commandTitle,
                exitCode = null,
                startTime = System.currentTimeMillis(),
                endTime = null,
                logs = listOf(
                    ShellLogEntry(
                        text = "--- [ZX SHIZUKU SHELL RUNNER] Starting: $commandTitle ---",
                        type = LogType.SYSTEM
                    )
                )
            )

            // Step 1: Pre-validation of Shizuku Binder & Permission
            val isBinderAlive = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
            if (!isBinderAlive) {
                appendLog("Error: Shizuku service is not active. Please start Shizuku service first.", LogType.ERROR)
                _state.value = _state.value.copy(
                    status = ExecutionStatus.FAILED,
                    exitCode = -1,
                    endTime = System.currentTimeMillis()
                )
                onComplete?.invoke(-1)
                return@launch
            }

            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }

            if (!hasPermission) {
                appendLog("Error: Shizuku permission has not been granted to ZX Shell Runner.", LogType.ERROR)
                appendLog("Please grant Shizuku permission in the Shizuku Guide tab or Shizuku app.", LogType.SYSTEM)
                _state.value = _state.value.copy(
                    status = ExecutionStatus.FAILED,
                    exitCode = -1,
                    endTime = System.currentTimeMillis()
                )
                onComplete?.invoke(-1)
                return@launch
            }

            val shellBinary = detectShell()
            appendLog("Backend: Shizuku Privileged Process (Shell: $shellBinary)", LogType.SYSTEM)
            appendLog("Executing script...", LogType.SYSTEM)

            _state.value = _state.value.copy(status = ExecutionStatus.RUNNING)

            var process: Process? = null
            var exitVal = -1
            var wasStopped = false

            try {
                // Real Shizuku privileged process creation using Shizuku API
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                process = newProcessMethod.invoke(
                    null,
                    arrayOf(shellBinary, "-c", scriptOrCommand),
                    null,
                    null
                ) as Process

                activeProcess = process

                val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

                // Launch concurrent stderr reader
                val stderrJob = launch(Dispatchers.IO) {
                    try {
                        var errLine: String? = null
                        while (isActive && stderrReader.readLine().also { errLine = it } != null) {
                            errLine?.let { appendLog(it, LogType.STDERR) }
                        }
                    } catch (_: Exception) {
                        // Stream closed or process destroyed
                    } finally {
                        try { stderrReader.close() } catch (_: Exception) {}
                    }
                }

                // Read stdout on current coroutine
                try {
                    var outLine: String? = null
                    while (isActive && stdoutReader.readLine().also { outLine = it } != null) {
                        outLine?.let { appendLog(it, LogType.STDOUT) }
                    }
                } catch (_: Exception) {
                    // Stream closed or process destroyed
                } finally {
                    try { stdoutReader.close() } catch (_: Exception) {}
                }

                stderrJob.join()

                // Wait for process exit code
                exitVal = process.waitFor()
            } catch (e: InterruptedException) {
                wasStopped = true
                appendLog("Process interrupted / stopped by user.", LogType.SYSTEM)
            } catch (e: Exception) {
                appendLog("Execution error: ${e.localizedMessage ?: e.message}", LogType.ERROR)
                exitVal = -1
            } finally {
                try {
                    process?.destroy()
                } catch (_: Exception) {}
                activeProcess = null
            }

            val finalEndTime = System.currentTimeMillis()
            if (wasStopped || _state.value.status == ExecutionStatus.STOPPED) {
                _state.value = _state.value.copy(
                    status = ExecutionStatus.STOPPED,
                    exitCode = exitVal,
                    endTime = finalEndTime
                )
                appendLog("--- [PROCESS STOPPED] ---", LogType.SYSTEM)
                onComplete?.invoke(exitVal)
            } else if (exitVal == 0) {
                _state.value = _state.value.copy(
                    status = ExecutionStatus.SUCCESS,
                    exitCode = 0,
                    endTime = finalEndTime
                )
                appendLog("--- [PROCESS FINISHED] Exit code: 0 (SUCCESS) ---", LogType.SYSTEM)
                onComplete?.invoke(0)
            } else {
                _state.value = _state.value.copy(
                    status = ExecutionStatus.FAILED,
                    exitCode = exitVal,
                    endTime = finalEndTime
                )
                appendLog("--- [PROCESS FINISHED] Exit code: $exitVal (FAILED) ---", LogType.ERROR)
                onComplete?.invoke(exitVal)
            }
        }
    }

    /**
     * Real STOP: terminates the privileged process, closes streams, updates exit state.
     */
    fun stopExecution() {
        if (activeProcess != null || _state.value.status == ExecutionStatus.RUNNING) {
            _state.value = _state.value.copy(status = ExecutionStatus.STOPPED)
            try {
                activeProcess?.destroy()
            } catch (_: Exception) {}
            activeProcess = null
            executionJob?.cancel()
            executionJob = null
            appendLog("--- Process killed by user. Streams closed. ---", LogType.SYSTEM)
        }
    }

    fun clearLogs() {
        _state.value = _state.value.copy(
            logs = emptyList(),
            exitCode = null,
            status = if (_state.value.status == ExecutionStatus.RUNNING) ExecutionStatus.RUNNING else ExecutionStatus.IDLE
        )
    }

    fun setAutoScroll(enabled: Boolean) {
        _state.value = _state.value.copy(autoScroll = enabled)
    }

    private fun appendLog(text: String, type: LogType) {
        val entry = ShellLogEntry(text = text, type = type)
        val currentLogs = _state.value.logs
        // Limit max logs in memory to 2000 to prevent OOM
        val updatedLogs = if (currentLogs.size > 2000) {
            currentLogs.drop(currentLogs.size - 1800) + entry
        } else {
            currentLogs + entry
        }
        _state.value = _state.value.copy(logs = updatedLogs)
    }

    fun getAllLogsFormatted(): String {
        return _state.value.logs.joinToString("\n") { log ->
            when (log.type) {
                LogType.STDOUT -> log.text
                LogType.STDERR -> "[STDERR] ${log.text}"
                LogType.SYSTEM -> "[SYSTEM] ${log.text}"
                LogType.ERROR -> "[ERROR] ${log.text}"
            }
        }
    }
}

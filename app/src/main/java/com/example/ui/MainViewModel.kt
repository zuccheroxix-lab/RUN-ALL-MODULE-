package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ModuleRepository
import com.example.data.ValidationResult
import com.example.data.db.AppDatabase
import com.example.data.db.ModuleEntity
import com.example.shizuku.ExecutionState
import com.example.shizuku.ExecutionStatus
import com.example.shizuku.ShellExecutor
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    MODULES,
    IMPORT,
    TERMINAL,
    COMMAND,
    SHIZUKU
}

data class PendingRunConfirmation(
    val module: ModuleEntity,
    val commandText: String,
    val warnings: List<String>
)

data class ImportPreviewState(
    val uri: Uri? = null,
    val fileName: String = "",
    val fileSize: Long = 0L,
    val validationResult: ValidationResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val repository = ModuleRepository(database.moduleDao(), context)
    val shizukuManager = ShizukuManager(context)
    val shellExecutor = ShellExecutor(viewModelScope)

    val modules: StateFlow<List<ModuleEntity>> = repository.allModules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val shizukuState: StateFlow<ShizukuState> = shizukuManager.state
    val executionState: StateFlow<ExecutionState> = shellExecutor.state

    private val _selectedTab = MutableStateFlow(AppTab.MODULES)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _importState = MutableStateFlow(ImportPreviewState())
    val importState: StateFlow<ImportPreviewState> = _importState.asStateFlow()

    private val _viewingModule = MutableStateFlow<ModuleEntity?>(null)
    val viewingModule: StateFlow<ModuleEntity?> = _viewingModule.asStateFlow()

    private val _pendingRunConfirmation = MutableStateFlow<PendingRunConfirmation?>(null)
    val pendingRunConfirmation: StateFlow<PendingRunConfirmation?> = _pendingRunConfirmation.asStateFlow()

    private val _manualCommandInput = MutableStateFlow("id")
    val manualCommandInput: StateFlow<String> = _manualCommandInput.asStateFlow()

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setManualCommand(command: String) {
        _manualCommandInput.value = command
    }

    /**
     * SAF Document selected by user.
     */
    fun onFileSelected(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportPreviewState(
                uri = uri,
                isLoading = true
            )

            // Take persistable permission
            repository.takePersistableUriPermission(uri)

            val (name, size) = repository.getUriMetadata(uri)
            val validation = repository.readAndValidateScript(uri)

            _importState.value = ImportPreviewState(
                uri = uri,
                fileName = name,
                fileSize = size,
                validationResult = validation,
                isLoading = false,
                errorMessage = if (validation.status == "ERROR") validation.message else null
            )
        }
    }

    fun clearImportSelection() {
        _importState.value = ImportPreviewState()
    }

    /**
     * Save the imported module into Room Database.
     */
    fun saveImportedModule(onSuccess: (Long) -> Unit) {
        val current = _importState.value
        val uri = current.uri ?: return
        val validation = current.validationResult

        viewModelScope.launch {
            val entity = ModuleEntity(
                name = current.fileName.ifEmpty { "module.sh" },
                uri = uri.toString(),
                size = current.fileSize,
                importDate = System.currentTimeMillis(),
                validationStatus = validation?.status ?: "VALID",
                validationMessage = validation?.message ?: "",
                previewContent = validation?.preview ?: ""
            )

            val newId = repository.insertModule(entity)
            Toast.makeText(context, "Module '${entity.name}' saved to library!", Toast.LENGTH_SHORT).show()
            _importState.value = ImportPreviewState()
            _selectedTab.value = AppTab.MODULES
            onSuccess(newId)
        }
    }

    fun viewModule(module: ModuleEntity) {
        _viewingModule.value = module
    }

    fun dismissViewModule() {
        _viewingModule.value = null
    }

    fun deleteModule(module: ModuleEntity) {
        viewModelScope.launch {
            repository.deleteModule(module)
            Toast.makeText(context, "Module deleted.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Request pre-run validation before actual execution.
     */
    fun requestRunModule(module: ModuleEntity) {
        viewModelScope.launch {
            // Re-read file to verify accessibility
            val uri = Uri.parse(module.uri)
            val validation = repository.readAndValidateScript(uri)

            if (validation.status == "ERROR") {
                Toast.makeText(context, "Error: ${validation.message}", Toast.LENGTH_LONG).show()
                return@launch
            }

            val warningsList = mutableListOf<String>()
            if (validation.status == "WARNING") {
                warningsList.add(validation.message)
            }

            _pendingRunConfirmation.value = PendingRunConfirmation(
                module = module,
                commandText = validation.fullContent.ifEmpty { module.previewContent },
                warnings = warningsList
            )
        }
    }

    fun dismissRunConfirmation() {
        _pendingRunConfirmation.value = null
    }

    /**
     * Confirmed run after pre-run dialog.
     */
    fun confirmRunModule() {
        val pending = _pendingRunConfirmation.value ?: return
        _pendingRunConfirmation.value = null
        _selectedTab.value = AppTab.TERMINAL

        shellExecutor.execute(
            commandTitle = pending.module.name,
            scriptOrCommand = pending.commandText
        ) { exitCode ->
            viewModelScope.launch {
                repository.updateLastRun(pending.module.id, exitCode)
            }
        }
    }

    /**
     * Run manual shell command via Shizuku.
     */
    fun runManualCommand(command: String) {
        val cleanCmd = command.trim()
        if (cleanCmd.isEmpty()) return

        _selectedTab.value = AppTab.TERMINAL
        shellExecutor.execute(
            commandTitle = cleanCmd,
            scriptOrCommand = cleanCmd
        )
    }

    fun stopExecution() {
        shellExecutor.stopExecution()
    }

    fun clearTerminal() {
        shellExecutor.clearLogs()
    }

    fun toggleAutoScroll(enabled: Boolean) {
        shellExecutor.setAutoScroll(enabled)
    }

    fun copyTerminalOutput() {
        val text = shellExecutor.getAllLogsFormatted()
        if (text.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Shell Output", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No output to copy", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareTerminalOutput() {
        val text = shellExecutor.getAllLogsFormatted()
        if (text.isNotEmpty()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Shell Output").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(shareIntent)
        }
    }

    fun checkShizuku() {
        shizukuManager.updateStatus()
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun openShizukuApp() {
        shizukuManager.openShizukuApp(context)
    }

    override fun onCleared() {
        super.onCleared()
        shizukuManager.cleanup()
    }
}

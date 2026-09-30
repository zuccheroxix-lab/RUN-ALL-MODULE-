package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.dialogs.PreRunConfirmDialog
import com.example.ui.dialogs.ViewModuleDialog
import com.example.ui.screens.CommandScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.ShizukuGuideScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalCyan

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                val modules by viewModel.modules.collectAsStateWithLifecycle()
                val shizukuState by viewModel.shizukuState.collectAsStateWithLifecycle()
                val executionState by viewModel.executionState.collectAsStateWithLifecycle()
                val importState by viewModel.importState.collectAsStateWithLifecycle()
                val viewingModule by viewModel.viewingModule.collectAsStateWithLifecycle()
                val pendingRunConfirmation by viewModel.pendingRunConfirmation.collectAsStateWithLifecycle()
                val manualCommandInput by viewModel.manualCommandInput.collectAsStateWithLifecycle()

                // Handle system back navigation when not on MODULES tab
                if (selectedTab != AppTab.MODULES) {
                    BackHandler {
                        viewModel.selectTab(AppTab.MODULES)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "ZX SHIZUKU SHELL",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.5.sp,
                                        color = TerminalCyan
                                    )
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == AppTab.MODULES,
                                onClick = { viewModel.selectTab(AppTab.MODULES) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = "Modules",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("Modules", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_modules")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.IMPORT,
                                onClick = { viewModel.selectTab(AppTab.IMPORT) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = "Import",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("Import", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_import")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.COMMAND,
                                onClick = { viewModel.selectTab(AppTab.COMMAND) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = "Command",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("Command", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_command")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.TERMINAL,
                                onClick = { viewModel.selectTab(AppTab.TERMINAL) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Terminal",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("Terminal", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_terminal")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppTab.SHIZUKU,
                                onClick = { viewModel.selectTab(AppTab.SHIZUKU) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Shizuku",
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = { Text("Shizuku", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_shizuku")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        AppTab.MODULES -> {
                            ModulesScreen(
                                modules = modules,
                                shizukuState = shizukuState,
                                onCheckShizuku = { viewModel.checkShizuku() },
                                onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
                                onOpenShizukuApp = { viewModel.openShizukuApp() },
                                onViewModule = { viewModel.viewModule(it) },
                                onRunModule = { viewModel.requestRunModule(it) },
                                onDeleteModule = { viewModel.deleteModule(it) },
                                onNavigateToImport = { viewModel.selectTab(AppTab.IMPORT) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppTab.IMPORT -> {
                            ImportScreen(
                                importState = importState,
                                onFileSelected = { viewModel.onFileSelected(it) },
                                onSaveModule = { viewModel.saveImportedModule {} },
                                onRunDirectly = { viewModel.requestRunModule(it) },
                                onClearSelection = { viewModel.clearImportSelection() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppTab.COMMAND -> {
                            CommandScreen(
                                commandInput = manualCommandInput,
                                onCommandChange = { viewModel.setManualCommand(it) },
                                onRunCommand = { viewModel.runManualCommand(it) },
                                shizukuState = shizukuState,
                                onCheckShizuku = { viewModel.checkShizuku() },
                                onRequestPermission = { viewModel.requestShizukuPermission() },
                                onOpenShizukuApp = { viewModel.openShizukuApp() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppTab.TERMINAL -> {
                            TerminalScreen(
                                state = executionState,
                                onStopExecution = { viewModel.stopExecution() },
                                onClearTerminal = { viewModel.clearTerminal() },
                                onCopyOutput = { viewModel.copyTerminalOutput() },
                                onShareOutput = { viewModel.shareTerminalOutput() },
                                onToggleAutoScroll = { viewModel.toggleAutoScroll(it) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        AppTab.SHIZUKU -> {
                            ShizukuGuideScreen(
                                shizukuState = shizukuState,
                                onCheckShizuku = { viewModel.checkShizuku() },
                                onRequestPermission = { viewModel.requestShizukuPermission() },
                                onOpenShizukuApp = { viewModel.openShizukuApp() },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    // Dialogs
                    pendingRunConfirmation?.let { confirmation ->
                        PreRunConfirmDialog(
                            confirmation = confirmation,
                            onDismiss = { viewModel.dismissRunConfirmation() },
                            onConfirm = { viewModel.confirmRunModule() }
                        )
                    }

                    viewingModule?.let { module ->
                        ViewModuleDialog(
                            module = module,
                            onDismiss = { viewModel.dismissViewModule() },
                            onRun = { viewModel.requestRunModule(module) }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically check Shizuku status when returning to app (e.g. from Shizuku manager app)
        viewModel.checkShizuku()
    }
}

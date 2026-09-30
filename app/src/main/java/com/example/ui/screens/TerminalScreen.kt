package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ExecutionState
import com.example.shizuku.ExecutionStatus
import com.example.shizuku.LogType
import com.example.shizuku.ShellLogEntry
import com.example.ui.theme.TerminalAmber
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed

@Composable
fun TerminalScreen(
    state: ExecutionState,
    onStopExecution: () -> Unit,
    onClearTerminal: () -> Unit,
    onCopyOutput: () -> Unit,
    onShareOutput: () -> Unit,
    onToggleAutoScroll: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Handle auto-scroll to bottom when new logs arrive
    LaunchedEffect(state.logs.size, state.autoScroll) {
        if (state.autoScroll && state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Status Bar Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("terminal_status_header"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Indicator
                val (statusColor, statusLabel) = when (state.status) {
                    ExecutionStatus.IDLE -> Pair(Color.Gray, "IDLE")
                    ExecutionStatus.CHECKING_SHIZUKU -> Pair(TerminalAmber, "CHECKING SHIZUKU")
                    ExecutionStatus.READY -> Pair(TerminalCyan, "READY")
                    ExecutionStatus.RUNNING -> Pair(TerminalCyan, "RUNNING")
                    ExecutionStatus.SUCCESS -> Pair(TerminalGreen, "SUCCESS")
                    ExecutionStatus.FAILED -> Pair(TerminalRed, "FAILED")
                    ExecutionStatus.STOPPED -> Pair(TerminalAmber, "STOPPED")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                    )
                }

                // Exit Code Display
                if (state.exitCode != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (state.exitCode == 0) TerminalGreen.copy(alpha = 0.2f) else TerminalRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "EXIT: ${state.exitCode}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (state.exitCode == 0) TerminalGreen else TerminalRed
                            )
                        )
                    }
                }
            }

            if (state.currentCommandTitle.isNotEmpty()) {
                Text(
                    text = "Active: ${state.currentCommandTitle}",
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Console Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A0E14))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                .testTag("terminal_output_area")
        ) {
            if (state.logs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ZX SHIZUKU TERMINAL READY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TerminalCyan.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Run a module or command to stream real stdout / stderr here.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(state.logs, key = { it.id }) { log ->
                        TerminalLogLine(log = log)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Terminal Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // STOP Button
                Button(
                    onClick = onStopExecution,
                    enabled = state.status == ExecutionStatus.RUNNING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TerminalRed,
                        disabledContainerColor = TerminalRed.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("stop_process_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Process",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "STOP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Action buttons: Clear, Copy, Share
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClearTerminal,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("clear_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Terminal",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopyOutput,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("copy_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Output",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onShareOutput,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("share_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Log",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Auto-scroll toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Auto-scroll",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = state.autoScroll,
                            onCheckedChange = onToggleAutoScroll,
                            modifier = Modifier.testTag("auto_scroll_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogLine(log: ShellLogEntry) {
    val (textColor, prefix) = when (log.type) {
        LogType.STDOUT -> Pair(Color(0xFFE2E8F0), "")
        LogType.STDERR -> Pair(Color(0xFFFCA5A5), "[ERR] ")
        LogType.SYSTEM -> Pair(TerminalCyan, "")
        LogType.ERROR -> Pair(TerminalRed, "[FAIL] ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
        Text(
            text = log.text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = textColor
            )
        )
    }
}

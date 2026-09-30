package com.example.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

data class ShizukuState(
    val isBinderAvailable: Boolean = false,
    val hasPermission: Boolean = false,
    val version: Int = 0,
    val uid: Int = -1,
    val isAppInstalled: Boolean = false,
    val isChecking: Boolean = false,
    val statusMessage: String = "Checking Shizuku status..."
)

class ShizukuManager(private val context: Context) {

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        updateStatus()
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_PERMISSION) {
            val granted = grantResult == PackageManager.PERMISSION_GRANTED
            _state.value = _state.value.copy(
                hasPermission = granted,
                statusMessage = if (granted) "Shizuku permission granted!" else "Shizuku permission denied by user."
            )
            updateStatus()
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (_: Throwable) {
            // Shizuku not present in runtime yet
        }
        updateStatus()
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (_: Throwable) {
            // Ignored
        }
    }

    fun updateStatus() {
        val isInstalled = isShizukuAppInstalled(context)
        var binderActive = false
        var permissionGranted = false
        var version = 0
        var uid = -1
        var message = ""

        try {
            binderActive = Shizuku.pingBinder()
            if (binderActive) {
                version = Shizuku.getVersion()
                uid = Shizuku.getUid()
                permissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                message = if (permissionGranted) {
                    "Shizuku service is active (UID: $uid, v$version) - Privileged shell ready!"
                } else {
                    "Shizuku service is running, but permission is not yet granted to ZX Shell Runner."
                }
            } else {
                message = if (isInstalled) {
                    "Shizuku app is installed but the service is NOT running. Please start Shizuku service."
                } else {
                    "Shizuku is not installed. Please install Shizuku to execute privileged shell commands."
                }
            }
        } catch (e: Throwable) {
            binderActive = false
            permissionGranted = false
            message = "Shizuku check error: ${e.localizedMessage ?: e.message}"
        }

        _state.value = ShizukuState(
            isBinderAvailable = binderActive,
            hasPermission = permissionGranted,
            version = version,
            uid = uid,
            isAppInstalled = isInstalled,
            isChecking = false,
            statusMessage = message
        )
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    updateStatus()
                } else {
                    Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                }
            } else {
                updateStatus()
            }
        } catch (e: Throwable) {
            _state.value = _state.value.copy(
                statusMessage = "Cannot request permission: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    fun openShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"))
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(marketIntent)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isShizukuAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_PERMISSION = 1001
    }
}

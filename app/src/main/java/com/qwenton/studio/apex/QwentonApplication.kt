package com.qwenton.studio.apex

import android.app.Application
import android.util.Log
import com.qwenton.studio.apex.data.local.ApiKeyManager
import com.qwenton.studio.apex.data.local.ModelCatalogRepository
import com.qwenton.studio.apex.terminal.QwentonTerminalBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class QwentonApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var _terminalBridge: QwentonTerminalBridge? = null
    val terminalBridge: QwentonTerminalBridge
        get() = _terminalBridge ?: synchronized(this) {
            _terminalBridge ?: QwentonTerminalBridge.getInstance(this).also { _terminalBridge = it }
        }

    @Volatile
    private var _modelCatalogRepository: ModelCatalogRepository? = null
    val modelCatalogRepository: ModelCatalogRepository
        get() = _modelCatalogRepository ?: synchronized(this) {
            _modelCatalogRepository ?: ModelCatalogRepository(this).also { _modelCatalogRepository = it }
        }

    @Volatile
    private var _apiKeyManager: ApiKeyManager? = null
    val apiKeyManager: ApiKeyManager
        get() = _apiKeyManager ?: synchronized(this) {
            _apiKeyManager ?: ApiKeyManager(this).also { _apiKeyManager = it }
        }

    override fun onCreate() {
        super.onCreate()

        // 1. CrashGuard: Defensively log and persist any uncaught thread exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "CrashGuard intercepted unhandled exception on [${thread.name}]: ${throwable.message}", throwable)
            try {
                val crashFile = File(filesDir, "last_crash.log")
                crashFile.writeText("Thread: ${thread.name}\nError: ${throwable.message}\nStacktrace:\n${throwable.stackTraceToString()}")
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 2. Safe ApiKey initialization
        try {
            apiKeyManager.getOrCreateApiKey()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed initializing ApiKeyManager safely", t)
        }

        // 3. Safe Catalog initialization
        try {
            applicationScope.launch {
                try {
                    modelCatalogRepository.refreshCatalog()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed background catalog refresh", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed initializing ModelCatalogRepository safely", t)
        }

        // 4. Safe Terminal Bridge initialization
        try {
            terminalBridge.initializeSession()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed initializing QwentonTerminalBridge safely", t)
        }
    }

    companion object {
        private const val TAG = "QwentonApplication"
    }
}

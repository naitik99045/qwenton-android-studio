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

        try {
            apiKeyManager.getOrCreateApiKey()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed initializing ApiKeyManager safely", t)
        }

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

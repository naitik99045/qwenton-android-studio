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

    lateinit var terminalBridge: QwentonTerminalBridge
        private set

    lateinit var modelCatalogRepository: ModelCatalogRepository
        private set

    lateinit var apiKeyManager: ApiKeyManager
        private set

    override fun onCreate() {
        super.onCreate()

        try {
            apiKeyManager = ApiKeyManager(this)
            apiKeyManager.getOrCreateApiKey()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed initializing ApiKeyManager safely", t)
        }

        try {
            modelCatalogRepository = ModelCatalogRepository(this)
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
            terminalBridge = QwentonTerminalBridge.getInstance(this)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed initializing QwentonTerminalBridge safely", t)
        }
    }

    companion object {
        private const val TAG = "QwentonApplication"
    }
}

package com.qwenton.studio.apex

import android.app.Application
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
        terminalBridge = QwentonTerminalBridge.getInstance(this)
        modelCatalogRepository = ModelCatalogRepository(this)
        apiKeyManager = ApiKeyManager(this)

        // Seed API key if first launch
        apiKeyManager.getOrCreateApiKey()

        // Scan local catalog in background
        applicationScope.launch {
            modelCatalogRepository.refreshCatalog()
        }
    }
}

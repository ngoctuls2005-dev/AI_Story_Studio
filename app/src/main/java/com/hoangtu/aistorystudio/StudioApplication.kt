package com.hoangtu.aistorystudio

import android.app.Application
import com.hoangtu.aistorystudio.core.ai.settings.DataStoreAiProviderSettingsRepository
import com.hoangtu.aistorystudio.core.datastore.aiProviderDataStore
import com.hoangtu.aistorystudio.core.security.AndroidKeyStoreSecureKeyStore
import com.hoangtu.aistorystudio.feature.settings.SettingsViewModelFactory

class StudioApplication : Application() {
    val aiProviderSettingsRepository: DataStoreAiProviderSettingsRepository by lazy {
        DataStoreAiProviderSettingsRepository(
            dataStore = applicationContext.aiProviderDataStore,
            secureKeyStore = AndroidKeyStoreSecureKeyStore()
        )
    }

    val settingsViewModelFactory: SettingsViewModelFactory by lazy {
        SettingsViewModelFactory(aiProviderSettingsRepository)
    }
}

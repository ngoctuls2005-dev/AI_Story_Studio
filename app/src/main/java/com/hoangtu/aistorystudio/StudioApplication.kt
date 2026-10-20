package com.hoangtu.aistorystudio

import android.app.Application
import com.hoangtu.aistorystudio.core.ai.settings.DataStoreAiProviderSettingsRepository
import com.hoangtu.aistorystudio.core.database.StudioDatabase
import com.hoangtu.aistorystudio.core.database.repository.RoomCharacterRepository
import com.hoangtu.aistorystudio.core.database.repository.RoomConversationRepository
import com.hoangtu.aistorystudio.core.datastore.aiProviderDataStore
import com.hoangtu.aistorystudio.core.security.AndroidKeyStoreSecureKeyStore
import com.hoangtu.aistorystudio.feature.characters.CharacterManagementViewModel
import com.hoangtu.aistorystudio.feature.roleplay.RoleplayViewModel
import com.hoangtu.aistorystudio.feature.settings.SettingsViewModelFactory

class StudioApplication : Application() {
    val studioDatabase: StudioDatabase by lazy { StudioDatabase.getInstance(this) }

    val characterRepository: RoomCharacterRepository by lazy {
        RoomCharacterRepository(studioDatabase.characterDao())
    }

    val conversationRepository: RoomConversationRepository by lazy {
        RoomConversationRepository(studioDatabase, studioDatabase.conversationDao())
    }

    val characterViewModelFactory: CharacterManagementViewModel.Factory by lazy {
        CharacterManagementViewModel.Factory(characterRepository)
    }

    val roleplayViewModelFactory: RoleplayViewModel.Factory by lazy {
        RoleplayViewModel.Factory(conversationRepository, characterRepository)
    }

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

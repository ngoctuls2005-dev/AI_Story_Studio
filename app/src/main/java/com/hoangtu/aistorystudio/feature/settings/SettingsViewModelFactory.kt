package com.hoangtu.aistorystudio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderSettingsRepository

class SettingsViewModelFactory(
    private val repository: AiProviderSettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            throw IllegalArgumentException("Khong ho tro ViewModel nay")
        }
        return SettingsViewModel(repository) as T
    }
}

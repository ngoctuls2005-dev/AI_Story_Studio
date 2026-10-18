package com.hoangtu.aistorystudio.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.aiProviderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_provider_settings"
)

package com.hoangtu.aistorystudio.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Upsert
    suspend fun upsertProfile(profile: CharacterProfileEntity)

    @Query("SELECT * FROM character_profiles WHERE id = :id")
    suspend fun getProfile(id: String): CharacterProfileEntity?

    @Query("SELECT * FROM character_profiles ORDER BY updated_at DESC, id ASC")
    fun observeProfiles(): Flow<List<CharacterProfileEntity>>

    @Query("SELECT * FROM character_profiles WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC, id ASC")
    suspend fun searchProfiles(query: String): List<CharacterProfileEntity>

    @Query("DELETE FROM character_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String): Int

    @Upsert
    suspend fun upsertPersona(persona: UserPersonaEntity)

    @Query("SELECT * FROM user_personas WHERE id = :id")
    suspend fun getPersona(id: String): UserPersonaEntity?

    @Query("SELECT * FROM user_personas ORDER BY updated_at DESC, id ASC")
    fun observePersonas(): Flow<List<UserPersonaEntity>>

    @Query("DELETE FROM user_personas WHERE id = :id")
    suspend fun deletePersona(id: String): Int
}

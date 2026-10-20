package com.hoangtu.aistorystudio.core.database.repository

import android.database.sqlite.SQLiteConstraintException
import com.hoangtu.aistorystudio.core.database.dao.CharacterDao
import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import kotlinx.coroutines.flow.Flow

enum class CharacterDeleteResult {
    DELETED,
    NOT_FOUND,
    REFERENCED
}

interface CharacterRepository {
    suspend fun saveProfile(profile: CharacterProfileEntity)
    suspend fun getProfile(id: String): CharacterProfileEntity?
    fun observeProfiles(): Flow<List<CharacterProfileEntity>>
    suspend fun searchProfiles(query: String): List<CharacterProfileEntity>
    suspend fun deleteProfile(id: String): CharacterDeleteResult
    suspend fun savePersona(persona: UserPersonaEntity)
    suspend fun getPersona(id: String): UserPersonaEntity?
    fun observePersonas(): Flow<List<UserPersonaEntity>>
    suspend fun deletePersona(id: String): CharacterDeleteResult
    suspend fun setDefaultPersona(id: String): Boolean
}

class RoomCharacterRepository(
    private val dao: CharacterDao
) : CharacterRepository {
    override suspend fun saveProfile(profile: CharacterProfileEntity) = dao.upsertProfile(profile)
    override suspend fun getProfile(id: String) = dao.getProfile(id)
    override fun observeProfiles() = dao.observeProfiles()
    override suspend fun searchProfiles(query: String) = dao.searchProfiles(query.trim())
    override suspend fun deleteProfile(id: String): CharacterDeleteResult = try {
        if (dao.deleteProfile(id) > 0) CharacterDeleteResult.DELETED else CharacterDeleteResult.NOT_FOUND
    } catch (_: SQLiteConstraintException) {
        CharacterDeleteResult.REFERENCED
    }
    override suspend fun savePersona(persona: UserPersonaEntity) = dao.upsertPersona(persona)
    override suspend fun getPersona(id: String) = dao.getPersona(id)
    override fun observePersonas() = dao.observePersonas()
    override suspend fun deletePersona(id: String): CharacterDeleteResult = try {
        if (dao.deletePersona(id) > 0) CharacterDeleteResult.DELETED else CharacterDeleteResult.NOT_FOUND
    } catch (_: SQLiteConstraintException) {
        CharacterDeleteResult.REFERENCED
    }

    override suspend fun setDefaultPersona(id: String) = dao.setDefaultPersona(id)
}

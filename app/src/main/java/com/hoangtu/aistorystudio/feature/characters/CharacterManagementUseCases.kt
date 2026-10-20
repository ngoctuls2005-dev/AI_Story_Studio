package com.hoangtu.aistorystudio.feature.characters

import com.hoangtu.aistorystudio.core.database.entity.CharacterProfileEntity
import com.hoangtu.aistorystudio.core.database.entity.UserPersonaEntity
import com.hoangtu.aistorystudio.core.database.repository.CharacterDeleteResult
import com.hoangtu.aistorystudio.core.database.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow

class CharacterManagementUseCases(
    private val repository: CharacterRepository
) {
    fun observeProfiles(): Flow<List<CharacterProfileEntity>> = repository.observeProfiles()

    fun observePersonas(): Flow<List<UserPersonaEntity>> = repository.observePersonas()

    suspend fun getProfile(id: String): CharacterProfileEntity? = repository.getProfile(id)

    suspend fun getPersona(id: String): UserPersonaEntity? = repository.getPersona(id)

    suspend fun searchProfiles(query: String): List<CharacterProfileEntity> = repository.searchProfiles(query)

    suspend fun saveProfile(profile: CharacterProfileEntity) {
        require(profile.name.isNotBlank()) { "Tên nhân vật không được để trống." }
        require(profile.age in CharacterProfileValidator.ADULT_AGE..CharacterProfileValidator.MAX_AGE) {
            "Tuổi nhân vật phải từ 18 đến 120."
        }
        repository.saveProfile(profile)
    }

    suspend fun savePersona(persona: UserPersonaEntity) {
        require(UserPersonaValidator.validate(persona.name) == null) {
            UserPersonaValidator.validate(persona.name).orEmpty()
        }
        repository.savePersona(persona)
    }

    suspend fun deleteProfile(id: String): CharacterDeleteResult = repository.deleteProfile(id)

    suspend fun deletePersona(id: String): CharacterDeleteResult = repository.deletePersona(id)

    suspend fun setDefaultPersona(id: String): Boolean = repository.setDefaultPersona(id)
}

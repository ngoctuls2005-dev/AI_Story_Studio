package com.hoangtu.aistorystudio.feature.characters

data class CharacterProfileInput(
    val name: String,
    val age: String
)

object CharacterProfileValidator {
    fun validate(input: CharacterProfileInput): String? {
        if (input.name.isBlank()) return "Vui lòng nhập tên nhân vật."
        if (input.name.trim().length > MAX_NAME_LENGTH) return "Tên nhân vật không được quá 80 ký tự."
        val age = input.age.toIntOrNull() ?: return "Tuổi phải là một số nguyên."
        if (age < ADULT_AGE) return "Nhân vật phải từ 18 tuổi trở lên."
        if (age > MAX_AGE) return "Tuổi không được lớn hơn 120."
        return null
    }

    const val ADULT_AGE = 18
    const val MAX_AGE = 120
    const val MAX_NAME_LENGTH = 80
}

object UserPersonaValidator {
    fun validate(name: String): String? {
        if (name.isBlank()) return "Vui lòng nhập tên Persona."
        if (name.trim().length > CharacterProfileValidator.MAX_NAME_LENGTH) {
            return "Tên Persona không được quá 80 ký tự."
        }
        return null
    }
}

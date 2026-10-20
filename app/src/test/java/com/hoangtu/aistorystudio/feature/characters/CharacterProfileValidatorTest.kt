package com.hoangtu.aistorystudio.feature.characters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterProfileValidatorTest {
    @Test
    fun requiresNameAndAdultAge() {
        assertEquals(
            "Vui lòng nhập tên nhân vật.",
            CharacterProfileValidator.validate(CharacterProfileInput("  ", "18"))
        )
        assertEquals(
            "Nhân vật phải từ 18 tuổi trở lên.",
            CharacterProfileValidator.validate(CharacterProfileInput("Linh", "17"))
        )
        assertEquals(
            "Tuổi phải là một số nguyên.",
            CharacterProfileValidator.validate(CharacterProfileInput("Linh", "mười tám"))
        )
        assertEquals(
            "Tuổi không được lớn hơn 120.",
            CharacterProfileValidator.validate(CharacterProfileInput("Linh", "121"))
        )
    }

    @Test
    fun acceptsAdultCharacterAndValidPersonaName() {
        assertNull(CharacterProfileValidator.validate(CharacterProfileInput("  Nguyễn An  ", "18")))
        assertNull(UserPersonaValidator.validate("  Người kể  "))
        assertEquals("Vui lòng nhập tên Persona.", UserPersonaValidator.validate(" "))
    }
}

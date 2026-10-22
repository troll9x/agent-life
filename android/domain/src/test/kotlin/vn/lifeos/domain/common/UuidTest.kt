package vn.lifeos.domain.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UuidTest {
    @Test
    fun onlyCanonicalIdsPassAndGeneratorCanBeInjected() {
        val canonical = "123e4567-e89b-42d3-a456-426614174000"
        val fixed = IdGenerator { Uuid.parse(canonical) }
        assertEquals(canonical, fixed.newId().value)
        assertFailsWith<IllegalArgumentException> { Uuid.parse(canonical.uppercase()) }
        assertFailsWith<IllegalArgumentException> { Uuid.parse("not-an-id") }
    }
}

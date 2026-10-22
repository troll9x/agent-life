package vn.lifeos.domain.common

import java.util.UUID

/** Canonical UUID string used for locally created entity IDs. */
@JvmInline
value class Uuid private constructor(val value: String) {
    companion object {
        fun parse(value: String): Uuid {
            val canonical = UUID.fromString(value).toString()
            require(value == canonical) { "ID must be a canonical UUID" }
            return Uuid(canonical)
        }
    }

    override fun toString(): String = value
}

fun interface IdGenerator {
    fun newId(): Uuid
}

object RandomIdGenerator : IdGenerator {
    override fun newId(): Uuid = Uuid.parse(UUID.randomUUID().toString())
}

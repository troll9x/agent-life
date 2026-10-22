package vn.lifeos.domain.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/** Deterministic fixtures for domain tests; no production credentials or personal data. */
object FixtureFactory {
    fun clock(at: String = "2026-10-19T01:59:00Z"): FakeClock = FakeClock(Instant.parse(at))

    fun idGenerator(id: String = "123e4567-e89b-42d3-a456-426614174000"): FakeIdGenerator =
        FakeIdGenerator(Uuid.parse(id))

    fun connectivity(online: Boolean = true): FakeConnectivity = FakeConnectivity(online)

    fun dispatcher(): TestDispatcher = StandardTestDispatcher()
}

class FakeClock(private var current: Instant) : Clock() {
    fun advanceTo(instant: Instant) {
        current = instant
    }

    override fun getZone() = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId): Clock = Clock.fixed(current, zone)

    override fun instant(): Instant = current
}

class FakeIdGenerator(private val id: Uuid) : IdGenerator {
    override fun newId(): Uuid = id
}

class FakeConnectivity(var online: Boolean) {
    fun isOnline(): Boolean = online
}

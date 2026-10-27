package vn.lifeos.app.planner

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerFormRulesTest {
    private val start = LocalDate.parse("2026-10-31")

    @Test fun editorRequiresOneBoundAndRejectsUnsupportedRule() {
        assertNull(parseRule("MONTHLY", "1", "", "", start))
        assertNull(parseRule("MONTHLY", "1", "2027-03-31", "3", start))
        assertNull(parseRule("YEARLY", "1", "", "3", start))
        assertEquals("MONTHLY;INTERVAL=1;COUNT=3", parseRule("MONTHLY", "1", "", "3", start))
    }
}

package io.github.elsaphaba.adguardwaker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleMathTest {
    @Test fun acceptsBoundaries() {
        assertEquals(1, ScheduleMath.parseMinutes("1"))
        assertEquals(720, ScheduleMath.parseMinutes(" 720 "))
    }
    @Test fun rejectsInvalidMinutes() {
        for (invalid in listOf("", "0", "-1", "721", "10080", "abc", "999999999999")) {
            assertThrows(NumberFormatException::class.java) { ScheduleMath.parseMinutes(invalid) }
        }
    }
    @Test fun computesIntervalsAndDueTimes() {
        assertEquals(301_000L, ScheduleMath.next(1_000L, 5))
        assertFalse(ScheduleMath.due(301_000L, 300_999L))
        assertTrue(ScheduleMath.due(301_000L, 301_000L))
        assertFalse(ScheduleMath.due(ScheduleMath.OFF, Long.MAX_VALUE))
        assertEquals(960_000L, ScheduleMath.next(900_000L, 1))
        assertEquals(43_200_000L, ScheduleMath.interval(10_080))
    }
}

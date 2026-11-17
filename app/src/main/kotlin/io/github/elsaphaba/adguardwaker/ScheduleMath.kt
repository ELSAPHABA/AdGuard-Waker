package io.github.elsaphaba.adguardwaker

internal object ScheduleMath {
    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 10_080
    const val OFF = Long.MAX_VALUE

    fun parseMinutes(input: String): Int {
        val value = input.trim().toIntOrNull() ?: throw NumberFormatException("Not an integer")
        if (value !in MIN_MINUTES..MAX_MINUTES) throw NumberFormatException("Out of range")
        return value
    }

    fun interval(minutes: Int): Long = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES) * 60_000L
    fun next(now: Long, minutes: Int): Long = now + interval(minutes)
    fun due(deadline: Long, now: Long): Boolean = deadline != OFF && deadline <= now
}

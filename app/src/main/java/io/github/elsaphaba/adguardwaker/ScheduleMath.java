package io.github.elsaphaba.adguardwaker;

final class ScheduleMath {
    static final int MIN_MINUTES = 1, MAX_MINUTES = 10080;
    static final long OFF = Long.MAX_VALUE;
    private ScheduleMath() {}
    static int parseMinutes(String s) {
        int n = Integer.parseInt(s.trim());
        if (n < MIN_MINUTES || n > MAX_MINUTES) throw new NumberFormatException();
        return n;
    }
    static long interval(int minutes) {
        return Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, minutes)) * 60000L;
    }
    static long next(long now, int minutes) { return now + interval(minutes); }
    static boolean due(long deadline, long now) { return deadline != OFF && deadline <= now; }
}

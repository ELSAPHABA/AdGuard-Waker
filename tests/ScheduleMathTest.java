package io.github.elsaphaba.adguardwaker;

public final class ScheduleMathTest {
    private static void expect(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        expect(ScheduleMath.parseMinutes("1") == 1);
        expect(ScheduleMath.parseMinutes("10080") == 10080);
        for (String invalid : new String[]{"", "0", "-1", "10081", "abc", "999999999999"}) {
            try { ScheduleMath.parseMinutes(invalid); throw new AssertionError(invalid); }
            catch (NumberFormatException expected) { }
        }
        expect(ScheduleMath.next(1_000L, 5) == 301_000L);
        expect(!ScheduleMath.due(301_000L, 300_999L));
        expect(ScheduleMath.due(301_000L, 301_000L));
        expect(!ScheduleMath.due(ScheduleMath.OFF, Long.MAX_VALUE));
        expect(ScheduleMath.next(900_000L, 1) == 960_000L);
        System.out.println("ScheduleMath tests passed");
    }
}

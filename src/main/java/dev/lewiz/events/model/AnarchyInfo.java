///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy
package dev.lewiz.events.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AnarchyInfo {

    public enum Mode {
        SOLO("Соло", 1, 7, 1),
        DUO("Дуо", 8, 17, 2),
        TRIO("Трио", 18, 24, 3),
        CLAN("Клан", 25, 30, 16);

        public final String label;
        public final int first;
        public final int last;
        public final int teamSize;

        Mode(String label, int first, int last, int teamSize) {
            this.label = label;
            this.first = first;
            this.last = last;
            this.teamSize = teamSize;
        }

        public static Mode of(int number) {
            for (Mode m : values()) {
                if (number >= m.first && number <= m.last) return m;
            }
            return null;
        }
    }

    private static final Pattern LITE = Pattern.compile("^LITE_ANARCHY_(\\d+)$");
    private static final Pattern LITE_NEW = Pattern.compile("^LITE_NEW_ANARCHY_(\\d+)$");

    public final String raw;
    public final boolean lite;
    public final boolean fresh;
    public final int number;
    public final Mode mode;

    private AnarchyInfo(String raw, boolean lite, boolean fresh, int number) {
        this.raw = raw;
        this.lite = lite;
        this.fresh = fresh;
        this.number = number;
        this.mode = lite ? Mode.of(number) : null;
    }

    public static AnarchyInfo parse(String key) {
        Matcher m = LITE.matcher(key);
        if (m.matches()) return new AnarchyInfo(key, true, false, Integer.parseInt(m.group(1)));
        m = LITE_NEW.matcher(key);
        if (m.matches()) return new AnarchyInfo(key, false, true, Integer.parseInt(m.group(1)));
        return new AnarchyInfo(key, false, false, -1);
    }

    public String title() {
        if (lite) return "Анархия " + number;
        if (fresh) return "Новая анархия " + number;
        return raw;
    }

    public String subtitle() {
        if (lite) return "Лайт" + (mode != null ? " · " + mode.label : "");
        return "";
    }

    public boolean canJoin() {
        return lite && mode != null;
    }

    public int sortKey() {
        if (lite) return number;
        if (fresh) return 1000 + number;
        return 5000;
    }
}

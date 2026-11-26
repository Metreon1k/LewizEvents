///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy

package dev.lewiz.events.gui;

import dev.lewiz.events.logic.AnarchyNavigator;
import dev.lewiz.events.logic.EventTracker;
import dev.lewiz.events.model.AnarchyInfo;
import dev.lewiz.events.net.ApiClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventsScreen extends Screen {

    private static final String DEV_URL = "https://t.me/LewizDev";

    private static final int BG = 0xFF0F0A17;
    private static final int PANEL_BORDER = 0xFF6B2BC4;
    private static final int ACCENT = 0xFF9B30FF;
    private static final int ACCENT_SOFT = 0xFF7A3FD0;
    private static final int ACCENT_TEXT = 0xFFB86BFF;
    private static final int CARD = 0xFF171021;
    private static final int CARD_HOVER = 0xFF21172F;
    private static final int CARD_BORDER = 0xFF2A1D3D;
    private static final int SELECTED = 0xFF2E1556;
    private static final int LINE = 0xFF251A36;
    private static final int TEXT = 0xFFF0EAFA;
    private static final int SUBTEXT = 0xFFCDBDEB;
    private static final int MUTED = 0xFF8E849C;
    private static final int ERROR = 0xFFFF6B6B;

    private static final int HEADER_H = 50;
    private static final int FOOTER_H = 24;
    private static final int SIDEBAR_W = 118;
    private static final int TAB_H = 32;
    private static final int CARD_H = 44;
    private static final int GAP = 6;
    private static final int BTN_W = 70;
    private static final int BTN_H = 18;
    private static final int CHIP_H = 16;

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private enum Tab {
        ACTIVE("Активные ивенты"),
        HISTORY("История событий");

        final String title;

        Tab(String title) {
            this.title = title;
        }
    }

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean contains(double x, double y) {
            return x >= x1 && x < x2 && y >= y1 && y < y2;
        }
    }

    private record Rarity(String label, int color) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private Tab tab = Tab.ACTIVE;
    private AnarchyInfo.Mode filter = null;
    private double scroll;
    private int px, py, pw, ph;

    public EventsScreen() {
        super(Text.literal("События"));
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 540);
        ph = Math.min(height - 16, 310);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        if (System.currentTimeMillis() - EventTracker.get().lastUpdate() > 5000) {
            ApiClient.refresh();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void render(DrawContext g, int mx, int my, float delta) {
        hits.clear();
        renderBackground(g, mx, my, delta);

        Draw.glow(g, px, py, px + pw, py + ph, 10, ACCENT);
        Draw.outlined(g, px, py, px + pw, py + ph, 10, BG, PANEL_BORDER);

        drawHeader(g, mx, my);
        g.fill(px + 12, py + HEADER_H, px + pw - 12, py + HEADER_H + 1, LINE);

        drawSidebar(g, mx, my);
        g.fill(px + SIDEBAR_W + 14, py + HEADER_H + 10, px + SIDEBAR_W + 15, py + ph - FOOTER_H - 8, LINE);

        drawContent(g, mx, my);
        drawFooter(g, mx, my);
    }

    private void drawHeader(DrawContext g, int mx, int my) {
        drawCalendarIcon(g, px + 16, py + 13);

        MatrixStack m = g.getMatrices();
        m.push();
        m.translate(px + 50f, py + 10f, 0f);
        m.scale(1.7f, 1.7f, 1f);
        g.drawText(textRenderer, Text.literal("События").formatted(Formatting.BOLD), 0, 0, ACCENT_TEXT, false);
        m.pop();
        g.drawText(textRenderer, "История событий со всех анархий", px + 50, py + 30, MUTED, false);

        int bx = px + pw - 32, by = py + 12;
        boolean hover = mx >= bx && mx < bx + 20 && my >= by && my < by + 20;
        Draw.outlined(g, bx, by, bx + 20, by + 20, 6, hover ? SELECTED : CARD, hover ? ACCENT : CARD_BORDER);
        for (int i = 0; i < 10; i++) {
            g.fill(bx + 5 + i, by + 5 + i, bx + 6 + i, by + 6 + i, TEXT);
            g.fill(bx + 14 - i, by + 5 + i, bx + 15 - i, by + 6 + i, TEXT);
        }
        hits.add(new Hit(bx, by, bx + 20, by + 20, this::close));
    }

    private void drawCalendarIcon(DrawContext g, int x, int y) {
        Draw.round(g, x, y + 3, x + 24, y + 24, 4, ACCENT);
        Draw.round(g, x + 2, y + 10, x + 22, y + 22, 3, BG);
        Draw.round(g, x + 5, y, x + 8, y + 7, 1, ACCENT);
        Draw.round(g, x + 16, y, x + 19, y + 7, 1, ACCENT);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                g.fill(x + 5 + col * 5, y + 12 + row * 5, x + 8 + col * 5, y + 15 + row * 5, ACCENT);
            }
        }
    }

    private void drawSidebar(DrawContext g, int mx, int my) {
        int x1 = px + 12;
        int x2 = x1 + SIDEBAR_W;
        int y = py + HEADER_H + 10;

        for (Tab t : Tab.values()) {
            boolean selected = t == tab;
            boolean hover = mx >= x1 && mx < x2 && my >= y && my < y + TAB_H;

            if (selected) Draw.glow(g, x1, y, x2, y + TAB_H, 8, ACCENT);
            Draw.outlined(g, x1, y, x2, y + TAB_H, 8, selected ? SELECTED : hover ? CARD_HOVER : CARD,
                    selected ? ACCENT : CARD_BORDER);

            int cx = x1 + 16, cy = y + TAB_H / 2;
            Draw.outlined(g, cx - 5, cy - 5, cx + 5, cy + 5, 5, selected ? SELECTED : CARD, selected ? ACCENT : MUTED);
            if (selected) Draw.round(g, cx - 2, cy - 2, cx + 2, cy + 2, 2, ACCENT);

            List<OrderedText> lines = textRenderer.wrapLines(Text.literal(t.title), SIDEBAR_W - 38);
            int ty = y + (TAB_H - lines.size() * 10 + 1) / 2;
            for (OrderedText line : lines) {
                g.drawText(textRenderer, line, x1 + 30, ty, selected ? TEXT : SUBTEXT, false);
                ty += 10;
            }

            final Tab target = t;
            hits.add(new Hit(x1, y, x2, y + TAB_H, () -> {
                tab = target;
                scroll = 0;
            }));
            y += TAB_H + 8;
        }
    }

    private void drawFilters(DrawContext g, int x1, int x2, int y, int mx, int my) {
        AnarchyInfo.Mode[] modes = AnarchyInfo.Mode.values();
        String[] full = {"Все", "Соло анархия", "Дуо анархия", "Трио анархия", "Клан анархия"};
        String[] shortLabels = {"Все", "Соло", "Дуо", "Трио", "Клан"};

        int total = -4;
        for (String s : full) total += textRenderer.getWidth(s) + 16 + 4;
        String[] labels = total <= x2 - x1 ? full : shortLabels;

        int x = x1;
        for (int i = 0; i < labels.length; i++) {
            final AnarchyInfo.Mode value = i == 0 ? null : modes[i - 1];
            int w = textRenderer.getWidth(labels[i]) + 16;
            boolean selected = filter == value;
            boolean hover = mx >= x && mx < x + w && my >= y && my < y + CHIP_H;

            Draw.outlined(g, x, y, x + w, y + CHIP_H, 8,
                    selected ? SELECTED : hover ? CARD_HOVER : CARD,
                    selected ? ACCENT : CARD_BORDER);
            g.drawText(textRenderer, labels[i], x + 8, y + 4, selected ? TEXT : SUBTEXT, false);

            hits.add(new Hit(x, y, x + w, y + CHIP_H, () -> {
                filter = value;
                scroll = 0;
            }));
            x += w + 4;
        }
    }

    private void drawContent(DrawContext g, int mx, int my) {
        EventTracker tracker = EventTracker.get();
        List<EventTracker.Entry> all = new ArrayList<>();
        for (EventTracker.Entry e : tab == Tab.ACTIVE ? tracker.active() : tracker.history()) {
            if (AnarchyInfo.parse(e.info().server()).mode != null) all.add(e);
        }

        List<EventTracker.Entry> list = new ArrayList<>();
        for (EventTracker.Entry e : all) {
            if (filter == null || AnarchyInfo.parse(e.info().server()).mode == filter) list.add(e);
        }

        int x1 = px + SIDEBAR_W + 26;
        int x2 = px + pw - 12;
        int top = py + HEADER_H + 10;
        int listTop = top + 50;
        int listBottom = py + ph - FOOTER_H - 4;

        Draw.round(g, x1, top + 1, x1 + 8, top + 9, 4, ACCENT);
        String head = tab.title + " · " + list.size();
        g.drawText(textRenderer, Text.literal(head).formatted(Formatting.BOLD), x1 + 14, top, ACCENT_TEXT, false);
        String hint = tab == Tab.ACTIVE
                ? "Время считается с момента, когда мод увидел ивент"
                : "Копится локально, пока запущен мод";
        g.drawText(textRenderer, trim(hint, x2 - x1 - 14), x1 + 14, top + 12, MUTED, false);

        drawFilters(g, x1, x2, top + 28, mx, my);

        if (list.isEmpty()) {
            drawEmpty(g, x1, listTop, x2, tracker, !all.isEmpty());
            return;
        }

        int contentH = list.size() * (CARD_H + GAP);
        int viewH = listBottom - listTop;
        int maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int cardRight = x2 - 8;
        boolean inList = my >= listTop && my < listBottom;
        long now = System.currentTimeMillis();

        g.enableScissor(x1 - 2, listTop, x2 + 2, listBottom);
        for (int i = 0; i < list.size(); i++) {
            int cy = listTop + i * (CARD_H + GAP) - (int) scroll;
            if (cy + CARD_H < listTop || cy > listBottom) continue;
            drawCard(g, list.get(i), x1, cy, cardRight, mx, my, inList, now, listTop, listBottom);
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int thumbH = Math.max(18, viewH * viewH / contentH);
            int thumbY = listTop + (int) ((viewH - thumbH) * (scroll / maxScroll));
            Draw.round(g, x2 - 3, listTop, x2, listBottom, 1, LINE);
            Draw.round(g, x2 - 3, thumbY, x2, thumbY + thumbH, 1, ACCENT);
        }
    }

    private void drawEmpty(DrawContext g, int x1, int y, int x2, EventTracker tracker, boolean filteredOut) {
        Draw.outlined(g, x1, y, x2, y + 52, 8, CARD, CARD_BORDER);
        String msg;
        if (filteredOut) {
            msg = "В этом режиме ивентов нет";
        } else if (tab == Tab.ACTIVE) {
            msg = tracker.lastUpdate() == 0 && tracker.lastError() == null
                    ? "Загрузка..." : "Сейчас нет активных ивентов";
        } else {
            msg = "Пока что история пуста";
        }
        int cx = (x1 + x2) / 2;
        g.drawText(textRenderer, msg, cx - textRenderer.getWidth(msg) / 2, y + 22, MUTED, false);
    }

    private void drawCard(DrawContext g, EventTracker.Entry e, int x1, int y, int x2, int mx, int my,
                          boolean inList, long now, int listTop, int listBottom) {
        boolean active = e.end() == 0;
        AnarchyInfo a = AnarchyInfo.parse(e.info().server());
        Rarity rarity = rarityOf(e.info().rare());
        int stripe = rarity != null ? rarity.color() : ACCENT;

        boolean hover = inList && mx >= x1 && mx < x2 && my >= y && my < y + CARD_H;
        Draw.outlined(g, x1, y, x2, y + CARD_H, 8, hover ? CARD_HOVER : CARD, hover ? ACCENT_SOFT : CARD_BORDER);
        Draw.round(g, x1 + 6, y + 8, x1 + 9, y + CARD_H - 8, 1, stripe);

        int tx = x1 + 18;
        int right = x2 - 10 - (active ? BTN_W + 8 : 0);
        int avail = right - tx;

        Text name = Text.literal(trim(e.info().name(), avail)).formatted(Formatting.BOLD);
        g.drawText(textRenderer, name, tx, y + 7, TEXT, false);
        if (rarity != null) {
            int rx = tx + textRenderer.getWidth(name) + 7;
            if (rx + textRenderer.getWidth(rarity.label()) < right) {
                g.drawText(textRenderer, rarity.label(), rx, y + 7, rarity.color(), false);
            }
        }

        String sub = a.subtitle().isEmpty() ? a.title() : a.title() + " · " + a.subtitle();
        g.drawText(textRenderer, trim(sub, avail), tx, y + 18, SUBTEXT, false);

        String start = HM.format(Instant.ofEpochMilli(e.start()));
        String time = active
                ? "Активен: " + duration(now - e.start()) + " · с " + start
                : start + " – " + HM.format(Instant.ofEpochMilli(e.end())) + " · длился " + duration(e.end() - e.start());
        g.drawText(textRenderer, trim(time, avail), tx, y + 29, MUTED, false);

        if (active) drawJoinButton(g, a, x2 - 10 - BTN_W, y + (CARD_H - BTN_H) / 2, mx, my, inList, listTop, listBottom);
    }

    private void drawJoinButton(DrawContext g, AnarchyInfo a, int bx, int by, int mx, int my,
                                boolean inList, int listTop, int listBottom) {
        boolean enabled = a.canJoin() && !AnarchyNavigator.get().isBusy();
        boolean hover = enabled && inList && mx >= bx && mx < bx + BTN_W && my >= by && my < by + BTN_H;

        if (hover) Draw.glow(g, bx, by, bx + BTN_W, by + BTN_H, 6, ACCENT);
        Draw.outlined(g, bx, by, bx + BTN_W, by + BTN_H, 6,
                !enabled ? 0xFF1E1828 : hover ? 0xFF7A2FE0 : 0xFF5322A8,
                !enabled ? CARD_BORDER : ACCENT);
        String label = "Перейти >";
        g.drawText(textRenderer, label, bx + (BTN_W - textRenderer.getWidth(label)) / 2, by + 5,
                enabled ? TEXT : MUTED, false);

        if (enabled && by >= listTop && by + BTN_H <= listBottom) {
            hits.add(new Hit(bx, by, bx + BTN_W, by + BTN_H, () -> {
                close();
                AnarchyNavigator.get().start(a);
            }));
        }
    }

    private void drawFooter(DrawContext g, int mx, int my) {
        EventTracker tracker = EventTracker.get();
        int y = py + ph - FOOTER_H;
        g.fill(px + 12, y, px + pw - 12, y + 1, LINE);

        String label = "Разработчики: ";
        String link = "t.me/LewizDev";
        int lw = textRenderer.getWidth(link);
        int lx = px + 16;
        int linkX = lx + textRenderer.getWidth(label);
        boolean lh = mx >= linkX && mx < linkX + lw && my >= y + 5 && my < y + 19;
        int linkColor = lh ? 0xFFDDB3FF : ACCENT_TEXT;
        g.drawText(textRenderer, label, lx, y + 9, MUTED, false);
        g.drawText(textRenderer, link, linkX, y + 9, linkColor, false);
        g.fill(linkX, y + 18, linkX + lw, y + 19, linkColor);
        hits.add(new Hit(linkX, y + 5, linkX + lw, y + 20, () -> Util.getOperatingSystem().open(URI.create(DEV_URL))));

        if (tracker.lastError() != null) {
            String err = "Ошибка: " + trim(tracker.lastError(), pw / 3);
            g.drawText(textRenderer, err, px + pw - 16 - textRenderer.getWidth(err), y + 9, ERROR, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.contains(mx, my)) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll -= vertical * 20;
        if (scroll < 0) scroll = 0;
        return true;
    }

    private String trim(String s, int width) {
        if (width <= 0) return "";
        if (textRenderer.getWidth(s) <= width) return s;
        return textRenderer.trimToWidth(s, width - textRenderer.getWidth("...")) + "...";
    }

    private static String duration(long ms) {
        long min = Math.max(0, ms) / 60000;
        if (min < 1) return "меньше минуты";
        if (min < 60) return min + " мин";
        return (min / 60) + " ч " + (min % 60) + " мин";
    }

    private static Rarity rarityOf(String raw) {
        if (raw == null) return null;
        String r = raw.toLowerCase(Locale.ROOT);
        if (r.contains("mythic") || r.contains("мифич")) return new Rarity("Мифический", 0xFFFF5577);
        if (r.contains("legend") || r.contains("легенд")) return new Rarity("Легендарный", 0xFFFFAA00);
        if (r.contains("epic") || r.contains("эпич")) return new Rarity("Эпический", 0xFFB44CFF);
        if (r.contains("rare") || r.contains("редк")) return new Rarity("Редкий", 0xFF55AAFF);
        if (r.contains("peaceful")) return new Rarity("Мирный", 0xFF55DD77);
        if (r.contains("normal") || r.contains("обычн")) return new Rarity("Обычный", 0xFFAAAAAA);
        return null;
    }
}
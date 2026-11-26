///  Мод написан студией LewizDev
///  Автор: Lewiz Dev (t.me/LewizDev)
///  t.me/LewizDev
///  discord.gg/27s7TMGeHy

package dev.lewiz.events.gui;

import net.minecraft.client.gui.DrawContext;

final class Draw {

    private Draw() {
    }

    static int alpha(int color, double factor) {
        int a = (int) Math.round(((color >>> 24) & 0xFF) * factor);
        return (a << 24) | (color & 0xFFFFFF);
    }

    static void round(DrawContext g, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(x2 - x1, y2 - y1) / 2));
        if (r == 0) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double inset = r - Math.sqrt(r * r - dy * dy);
            int full = (int) Math.ceil(inset);
            double cov = full - inset;
            int l = x1 + full;
            int rr = x2 - full;
            g.fill(l, y1 + i, rr, y1 + i + 1, color);
            g.fill(l, y2 - 1 - i, rr, y2 - i, color);
            if (full > 0 && cov > 0.04) {
                int c = alpha(color, cov);
                g.fill(l - 1, y1 + i, l, y1 + i + 1, c);
                g.fill(rr, y1 + i, rr + 1, y1 + i + 1, c);
                g.fill(l - 1, y2 - 1 - i, l, y2 - i, c);
                g.fill(rr, y2 - 1 - i, rr + 1, y2 - i, c);
            }
        }
        g.fill(x1, y1 + r, x2, y2 - r, color);
    }

    static void outlined(DrawContext g, int x1, int y1, int x2, int y2, int r, int fill, int border) {
        round(g, x1, y1, x2, y2, r, border);
        round(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), fill);
    }

    static void glow(DrawContext g, int x1, int y1, int x2, int y2, int r, int color) {
        for (int k = 5; k >= 1; k--) {
            round(g, x1 - k, y1 - k, x2 + k, y2 + k, r + k, alpha(color, 0.045));
        }
    }
}
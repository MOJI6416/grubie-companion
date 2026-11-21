package com.grubielauncher.companion.ui;

public final class Theme {
    public static final int PANEL = 0xF3161719;
    public static final int SURFACE = 0xFF1C1D20;
    public static final int RAISED = 0xFF25262A;
    public static final int HOVER = 0xFF2C2D32;
    public static final int BORDER = 0x17FFFFFF;
    public static final int TEXT = 0xFFF3F4F6;
    public static final int MUTED = 0xFFA3A5AA;
    public static final int FAINT = 0xFF8D8F94;
    public static final int PRIMARY = 0xFF7C5CFF;
    public static final int PRIMARY_HOVER = 0xFF8E72FF;
    public static final int PRIMARY_SOFT = 0x2E7C5CFF;
    public static final int SUCCESS = 0xFF34C38A;
    public static final int WARNING = 0xFFF0B43C;
    public static final int DANGER = 0xFFEF6B7C;
    public static final int INFO = 0xFF5FB0EF;
    public static final int SHADOW = 0x8C000000;
    public static final int BACKDROP = 0x73060709;

    public static final float TEXT_SM = 5.5f;
    public static final float TEXT_MD = 6.25f;
    public static final float TEXT_LG = 7.25f;
    public static final float TEXT_XL = 8.5f;
    public static final int MIN_TEXT_PX = 11;

    public static final float RADIUS_SM = 3.5f;
    public static final float RADIUS = 5f;
    public static final float RADIUS_LG = 7f;

    public static final long FAST_MS = 120;
    public static final long NORMAL_MS = 180;
    public static final long SLOW_MS = 260;

    private Theme() {
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.round(((argb >>> 24) & 0xff) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (argb & 0xffffff);
    }

    public static int mix(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(((from >>> 24) & 0xff) + (((to >>> 24) & 0xff) - ((from >>> 24) & 0xff)) * t);
        int r = Math.round(((from >> 16) & 0xff) + (((to >> 16) & 0xff) - ((from >> 16) & 0xff)) * t);
        int g = Math.round(((from >> 8) & 0xff) + (((to >> 8) & 0xff) - ((from >> 8) & 0xff)) * t);
        int b = Math.round((from & 0xff) + ((to & 0xff) - (from & 0xff)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}

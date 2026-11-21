package com.grubielauncher.companion.render;

import com.grubielauncher.companion.Companion;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public final class Text {
    public enum Weight {
        REGULAR("rubik-regular.ttf"),
        MEDIUM("rubik-medium.ttf"),
        SEMIBOLD("rubik-semibold.ttf");

        final String file;

        Weight(String file) {
            this.file = file;
        }
    }

    private static final int ATLAS = 1024;
    private static final int PAD = 1;
    private static final String ELLIPSIS = "…";

    private static final class Face {
        final ByteBuffer data;
        final STBTTFontinfo info;
        final int ascent;
        final int descent;
        final int lineGap;
        final int unitsPerEm;

        Face(ByteBuffer data, STBTTFontinfo info, int ascent, int descent, int lineGap, int unitsPerEm) {
            this.data = data;
            this.info = info;
            this.ascent = ascent;
            this.descent = descent;
            this.lineGap = lineGap;
            this.unitsPerEm = unitsPerEm;
        }
    }

    private static final class Glyph {
        int u;
        int v;
        int w;
        int h;
        int xoff;
        int yoff;
        float advance;
    }

    private static final Map<Weight, Face> FACES = new HashMap<>();
    private static final Map<Long, Glyph> GLYPHS = new HashMap<>();
    private static boolean failed;
    private static Tex atlas;
    private static int shelfX;
    private static int shelfY;
    private static int shelfH;
    private static boolean dirty;

    private Text() {
    }

    public static boolean available() {
        return !failed && face(Weight.REGULAR) != null;
    }

    public static int lineHeight(int px) {
        Face face = face(Weight.REGULAR);
        if (face == null) return px;
        float scale = (float) px / face.unitsPerEm;
        return Math.round((face.ascent - face.descent) * scale);
    }

    public static int ascent(int px, Weight weight) {
        Face face = face(weight);
        if (face == null) return px;
        return Math.round(face.ascent * ((float) px / face.unitsPerEm));
    }

    public static boolean supports(String text, Weight weight) {
        Face face = face(weight);
        if (face == null) return false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ' ' || cp == ' ') continue;
            if (STBTruetype.stbtt_FindGlyphIndex(face.info, cp) == 0) return false;
        }
        return true;
    }

    public static float width(String text, int px, Weight weight) {
        Face face = face(weight);
        if (face == null || !supports(text, weight)) return Gfx.vanillaWidth(text) * px / 9f;
        float scale = (float) px / face.unitsPerEm;
        float width = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer lsb = stack.mallocInt(1);
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                STBTruetype.stbtt_GetCodepointHMetrics(face.info, cp, advance, lsb);
                width += advance.get(0) * scale;
            }
        }
        return width;
    }

    public static String fit(String text, float maxWidth, int px, Weight weight) {
        if (text == null) return "";
        if (width(text, px, weight) <= maxWidth) return text;
        int end = text.length();
        while (end > 0) {
            end--;
            if (Character.isLowSurrogate(text.charAt(end)) && end > 0) end--;
            String candidate = text.substring(0, end).trim() + ELLIPSIS;
            if (width(candidate, px, weight) <= maxWidth) return candidate;
        }
        return ELLIPSIS;
    }

    public static void draw(Gfx gfx, String text, int x, int y, int px, Weight weight, int argb) {
        if (text == null || text.isEmpty()) return;
        Face face = face(weight);
        if (face == null || !supports(text, weight)) {
            drawVanilla(gfx, text, x, y, px, argb);
            return;
        }
        float scale = (float) px / face.unitsPerEm;
        int baseline = y + Math.round(face.ascent * scale);
        Glyph[] glyphs = new Glyph[text.length()];
        int count = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            glyphs[count++] = glyph(face, weight, px, cp);
        }
        if (dirty && atlas != null) {
            atlas.upload();
            dirty = false;
        }
        float pen = x;
        for (int i = 0; i < count; i++) {
            Glyph glyph = glyphs[i];
            if (glyph == null) continue;
            if (glyph.w > 0 && glyph.h > 0) {
                gfx.blit(atlas, Math.round(pen) + glyph.xoff, baseline + glyph.yoff, glyph.w, glyph.h, glyph.u, glyph.v, glyph.w, glyph.h, argb);
            }
            pen += glyph.advance;
        }
    }

    private static void drawVanilla(Gfx gfx, String text, int x, int y, int px, int argb) {
        float s = px / 9f * 1.15f;
        gfx.push();
        gfx.translate(x, y);
        gfx.scale(s);
        gfx.vanillaText(text, 0, 0, argb, false);
        gfx.pop();
    }

    public static void reset() {
        GLYPHS.clear();
        if (atlas != null) atlas.close();
        atlas = null;
        shelfX = 0;
        shelfY = 0;
        shelfH = 0;
        dirty = false;
    }

    private static Glyph glyph(Face face, Weight weight, int px, int cp) {
        long key = ((long) weight.ordinal() << 48) | ((long) px << 32) | (cp & 0xffffffffL);
        Glyph cached = GLYPHS.get(key);
        if (cached != null) return cached;
        if (atlas == null) {
            atlas = Tex.create("glyphs", ATLAS, ATLAS);
            atlas.clear();
        }
        float scale = (float) px / face.unitsPerEm;
        Glyph glyph = new Glyph();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int index = STBTruetype.stbtt_FindGlyphIndex(face.info, cp);
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer lsb = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphHMetrics(face.info, index, advance, lsb);
            glyph.advance = advance.get(0) * scale;
            IntBuffer x0 = stack.mallocInt(1);
            IntBuffer y0 = stack.mallocInt(1);
            IntBuffer x1 = stack.mallocInt(1);
            IntBuffer y1 = stack.mallocInt(1);
            STBTruetype.stbtt_GetGlyphBitmapBox(face.info, index, scale, scale, x0, y0, x1, y1);
            int w = x1.get(0) - x0.get(0);
            int h = y1.get(0) - y0.get(0);
            glyph.xoff = x0.get(0);
            glyph.yoff = y0.get(0);
            if (w > 0 && h > 0) {
                if (!reserve(w, h)) {
                    reset();
                    return glyph(face, weight, px, cp);
                }
                glyph.u = shelfX;
                glyph.v = shelfY;
                glyph.w = w;
                glyph.h = h;
                ByteBuffer bitmap = MemoryUtil.memAlloc(w * h);
                try {
                    STBTruetype.stbtt_MakeGlyphBitmap(face.info, bitmap, w, h, w, scale, scale, index);
                    for (int yy = 0; yy < h; yy++) {
                        for (int xx = 0; xx < w; xx++) {
                            int coverage = bitmap.get(yy * w + xx) & 0xff;
                            int a = coverage == 0 ? 0 : (int) Math.min(255, Math.round(Math.pow(coverage / 255.0, 0.85) * 255));
                            atlas.set(glyph.u + xx, glyph.v + yy, (a << 24) | 0xffffff);
                        }
                    }
                } finally {
                    MemoryUtil.memFree(bitmap);
                }
                shelfX += w + PAD;
                dirty = true;
            }
        }
        GLYPHS.put(key, glyph);
        return glyph;
    }

    private static boolean reserve(int w, int h) {
        if (w + PAD > ATLAS || h + PAD > ATLAS) return false;
        if (shelfX + w + PAD > ATLAS) {
            shelfY += shelfH + PAD;
            shelfX = 0;
            shelfH = 0;
        }
        if (shelfY + h + PAD > ATLAS) return false;
        shelfH = Math.max(shelfH, h);
        return true;
    }

    private static Face face(Weight weight) {
        if (failed) return null;
        Face face = FACES.get(weight);
        if (face != null) return face;
        try (InputStream stream = Text.class.getResourceAsStream("/assets/grubie_companion/font/" + weight.file)) {
            if (stream == null) throw new IOException("missing " + weight.file);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int read;
            while ((read = stream.read(buffer)) > 0) bytes.write(buffer, 0, read);
            byte[] raw = bytes.toByteArray();
            ByteBuffer data = MemoryUtil.memAlloc(raw.length);
            data.put(raw).flip();
            STBTTFontinfo info = STBTTFontinfo.malloc();
            if (!STBTruetype.stbtt_InitFont(info, data)) throw new IOException("stbtt_InitFont failed for " + weight.file);
            int ascent;
            int descent;
            int lineGap;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer a = stack.mallocInt(1);
                IntBuffer d = stack.mallocInt(1);
                IntBuffer g = stack.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(info, a, d, g);
                ascent = a.get(0);
                descent = d.get(0);
                lineGap = g.get(0);
            }
            float unitScale = STBTruetype.stbtt_ScaleForMappingEmToPixels(info, 1000f);
            int unitsPerEm = Math.max(1, Math.round(1000f / unitScale));
            face = new Face(data, info, ascent, descent, lineGap, unitsPerEm);
            FACES.put(weight, face);
            return face;
        } catch (IOException | RuntimeException | LinkageError e) {
            failed = true;
            Companion.LOGGER.warn("Grubie Companion font is unavailable, using the game font: {}", e.toString());
            return null;
        }
    }
}

package com.grubielauncher.companion.render;

import com.grubielauncher.companion.Companion;
import com.grubielauncher.companion.core.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Icons {
    private static Tex atlas;
    private static boolean failed;
    private static int[] sizes = new int[0];
    private static int[] rows = new int[0];
    private static final Map<String, Integer> INDEX = new HashMap<>();

    private Icons() {
    }

    public static void draw(Canvas canvas, String name, float x, float y, float size, int argb) {
        if (!load()) return;
        Integer index = INDEX.get(name);
        if (index == null) return;
        int want = canvas.px(size);
        int pick = sizes.length - 1;
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] >= want) {
                pick = i;
                break;
            }
        }
        int s = sizes[pick];
        int u = index * (s + 2) + 1;
        int v = rows[pick] + 1;
        int px = canvas.px(x);
        int py = canvas.px(y);
        canvas.gfx.blit(atlas, px, py, want, want, u, v, s, s, argb);
    }

    public static void reset() {
        if (atlas != null) atlas.close();
        atlas = null;
    }

    private static boolean load() {
        if (atlas != null) return true;
        if (failed) return false;
        try {
            byte[] png = read("/assets/grubie_companion/textures/icons.png");
            Json.Obj index = Json.parseObject(new String(read("/assets/grubie_companion/textures/icons.json"), StandardCharsets.UTF_8));
            if (index == null) throw new IOException("bad icons.json");
            List<Object> sizeList = list(index.raw("sizes"));
            List<Object> rowList = list(index.raw("rows"));
            List<Object> names = list(index.raw("names"));
            sizes = new int[sizeList.size()];
            rows = new int[rowList.size()];
            for (int i = 0; i < sizes.length; i++) sizes[i] = ((Number) sizeList.get(i)).intValue();
            for (int i = 0; i < rows.length; i++) rows[i] = ((Number) rowList.get(i)).intValue();
            INDEX.clear();
            for (int i = 0; i < names.size(); i++) INDEX.put(String.valueOf(names.get(i)), i);
            atlas = Tex.fromPng("icons", png);
            return true;
        } catch (IOException | RuntimeException e) {
            failed = true;
            Companion.LOGGER.warn("Grubie Companion icons are unavailable: {}", e.toString());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return value instanceof List ? (List<Object>) value : List.of();
    }

    private static byte[] read(String path) throws IOException {
        try (InputStream stream = Icons.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("missing " + path);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int read;
            while ((read = stream.read(buffer)) > 0) bytes.write(buffer, 0, read);
            return bytes.toByteArray();
        }
    }
}

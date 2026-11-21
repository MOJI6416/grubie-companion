package com.grubielauncher.companion.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class Tex {
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final ResourceLocation id;
    private final DynamicTexture texture;
    private final NativeImage image;
    private final int width;
    private final int height;
    private boolean closed;

    private Tex(String name, NativeImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
        String path = "dynamic/" + name + "_" + COUNTER.incrementAndGet();
        this.id = Ids.of(path);
        //? if >=1.21.5 {
        /*this.texture = new DynamicTexture(() -> "grubie_companion:" + path, image);
        *///?} else
        this.texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    public static Tex create(String name, int width, int height) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, true);
        return new Tex(name, image);
    }

    public static Tex fromPng(String name, byte[] png) throws IOException {
        NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
        return new Tex(name, image);
    }

    public ResourceLocation id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void set(int x, int y, int argb) {
        //? if >=1.21.2 {
        /*image.setPixel(x, y, argb);
        *///?} else {
        int abgr = (argb & 0xff00ff00) | ((argb >> 16) & 0xff) | ((argb & 0xff) << 16);
        image.setPixelRGBA(x, y, abgr);
        //?}
    }

    public int get(int x, int y) {
        //? if >=1.21.2 {
        /*return image.getPixel(x, y);
        *///?} else {
        int abgr = image.getPixelRGBA(x, y);
        return (abgr & 0xff00ff00) | ((abgr >> 16) & 0xff) | ((abgr & 0xff) << 16);
        //?}
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) set(x, y, 0);
        }
    }

    public void upload() {
        if (!closed) texture.upload();
    }

    public void close() {
        if (closed) return;
        closed = true;
        Minecraft.getInstance().getTextureManager().release(id);
    }
}

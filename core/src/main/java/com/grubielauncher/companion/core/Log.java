package com.grubielauncher.companion.core;

import java.util.function.BiConsumer;

public final class Log {
    private static volatile BiConsumer<String, String> sink = (level, message) -> {};

    private Log() {
    }

    public static void setSink(BiConsumer<String, String> next) {
        sink = next == null ? (level, message) -> {} : next;
    }

    public static void debug(String message) {
        sink.accept("debug", message);
    }

    public static void info(String message) {
        sink.accept("info", message);
    }

    public static void warn(String message) {
        sink.accept("warn", message);
    }
}

package com.grubielauncher.companion.core;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Map;

public final class CompanionEnv {
    public static final String ADDRESS = "GRUBIE_COMPANION_ADDRESS";
    public static final String TOKEN = "GRUBIE_COMPANION_TOKEN";
    public static final String STATS_DIR = "GRUBIE_COMPANION_STATS_DIR";

    public final String host;
    public final int port;
    public final String token;
    public final Path statsDir;

    private CompanionEnv(String host, int port, String token, Path statsDir) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.statsDir = statsDir;
    }

    public static CompanionEnv fromSystem() {
        return from(System.getenv());
    }

    public static CompanionEnv from(Map<String, String> env) {
        String address = trim(env.get(ADDRESS));
        String token = trim(env.get(TOKEN));
        if (address == null || token == null || token.length() < 16 || token.length() > 256) return null;

        int colon = address.lastIndexOf(':');
        if (colon <= 0 || colon == address.length() - 1) return null;
        String host = address.substring(0, colon);
        int port;
        try {
            port = Integer.parseInt(address.substring(colon + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (port <= 0 || port > 65535 || !isLoopback(host)) return null;

        Path statsDir = null;
        String rawDir = trim(env.get(STATS_DIR));
        if (rawDir != null) {
            try {
                Path candidate = Path.of(rawDir);
                if (candidate.isAbsolute()) statsDir = candidate;
            } catch (RuntimeException ignored) {
            }
        }

        return new CompanionEnv(host, port, token, statsDir);
    }

    static boolean isLoopback(String host) {
        if (!host.equals("127.0.0.1") && !host.equals("localhost") && !host.equals("::1")) return false;
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    private static String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

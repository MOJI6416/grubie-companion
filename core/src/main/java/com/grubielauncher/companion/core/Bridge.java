package com.grubielauncher.companion.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class Bridge {
    public interface Handler {
        void onMessage(Json.Obj message);

        void onConnected(boolean connected);
    }

    static final int MAX_LINE = 1 << 20;
    private static final long[] BACKOFF_MS = {1_000, 2_000, 5_000, 10_000, 30_000};

    private final String host;
    private final int port;
    private final Json.Obj hello;
    private final Handler handler;
    private final Object writeLock = new Object();
    private final Object sleepLock = new Object();

    private volatile Writer writer;
    private volatile Socket socket;
    private volatile boolean connected;
    private volatile boolean closed;
    private Thread thread;

    public Bridge(String host, int port, Json.Obj hello, Handler handler) {
        this.host = host;
        this.port = port;
        this.hello = hello;
        this.handler = handler;
    }

    public synchronized void start() {
        if (thread != null) return;
        thread = new Thread(this::run, "Grubie Companion bridge");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean send(Json.Obj message) {
        synchronized (writeLock) {
            Writer current = writer;
            if (current == null || !connected) return false;
            try {
                current.write(message.toString());
                current.write('\n');
                current.flush();
                return true;
            } catch (IOException e) {
                closeSocket();
                return false;
            }
        }
    }

    public void close() {
        closed = true;
        closeSocket();
        synchronized (sleepLock) {
            sleepLock.notifyAll();
        }
    }

    private void run() {
        int attempt = 0;
        while (!closed) {
            boolean welcomed = false;
            try {
                welcomed = connectOnce();
            } catch (IOException | RuntimeException e) {
                Log.debug("Launcher bridge: " + e.getMessage());
            } finally {
                closeSocket();
                if (connected) {
                    connected = false;
                    safeConnected(false);
                }
            }
            if (closed) break;
            attempt = welcomed ? 0 : attempt + 1;
            pause(BACKOFF_MS[Math.min(attempt, BACKOFF_MS.length - 1)]);
        }
    }

    private boolean connectOnce() throws IOException {
        Socket next = new Socket();
        socket = next;
        next.connect(new InetSocketAddress(host, port), 3_000);
        next.setTcpNoDelay(true);
        next.setSoTimeout(0);

        BufferedReader reader = new BufferedReader(new InputStreamReader(next.getInputStream(), StandardCharsets.UTF_8));
        synchronized (writeLock) {
            writer = new BufferedWriter(new OutputStreamWriter(next.getOutputStream(), StandardCharsets.UTF_8));
            writer.write(hello.toString());
            writer.write('\n');
            writer.flush();
        }

        boolean welcomed = false;
        String line;
        while (!closed && (line = readLine(reader, MAX_LINE)) != null) {
            Json.Obj message = Json.parseObject(line);
            if (message == null) continue;
            String type = Protocol.type(message);
            if ("welcome".equals(type)) {
                if (!welcomed) {
                    welcomed = true;
                    connected = true;
                    deliver(message);
                    safeConnected(true);
                }
                continue;
            }
            if (!welcomed) continue;
            deliver(message);
        }
        return welcomed;
    }

    private void deliver(Json.Obj message) {
        try {
            handler.onMessage(message);
        } catch (RuntimeException e) {
            Log.warn("Launcher message failed: " + e);
        }
    }

    private void safeConnected(boolean value) {
        try {
            handler.onConnected(value);
        } catch (RuntimeException e) {
            Log.warn("Launcher status handler failed: " + e);
        }
    }

    private void pause(long millis) {
        synchronized (sleepLock) {
            if (closed) return;
            try {
                sleepLock.wait(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                closed = true;
            }
        }
    }

    private void closeSocket() {
        synchronized (writeLock) {
            writer = null;
        }
        Socket current = socket;
        socket = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
            }
        }
    }

    static String readLine(Reader reader, int limit) throws IOException {
        StringBuilder line = new StringBuilder();
        while (true) {
            int next = reader.read();
            if (next == -1) return line.length() == 0 ? null : line.toString();
            if (next == '\n') return line.toString();
            if (next == '\r') continue;
            if (line.length() >= limit) throw new IOException("Line too long");
            line.append((char) next);
        }
    }
}

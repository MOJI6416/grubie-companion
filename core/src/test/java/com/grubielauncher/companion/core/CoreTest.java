package com.grubielauncher.companion.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void envNeedsLoopbackAddressAndToken() {
        String token = "0123456789abcdef0123456789abcdef";
        CompanionEnv env = CompanionEnv.from(Map.of(
                CompanionEnv.ADDRESS, "127.0.0.1:41234",
                CompanionEnv.TOKEN, token,
                CompanionEnv.STATS_DIR, Path.of("").toAbsolutePath().toString()));
        assertNotNull(env);
        assertEquals(41234, env.port);
        assertNotNull(env.statsDir);

        assertNull(CompanionEnv.from(Map.of()));
        assertNull(CompanionEnv.from(Map.of(CompanionEnv.ADDRESS, "10.0.0.5:41234", CompanionEnv.TOKEN, token)));
        assertNull(CompanionEnv.from(Map.of(CompanionEnv.ADDRESS, "127.0.0.1:0", CompanionEnv.TOKEN, token)));
        assertNull(CompanionEnv.from(Map.of(CompanionEnv.ADDRESS, "127.0.0.1:41234", CompanionEnv.TOKEN, "short")));
        CompanionEnv relative = CompanionEnv.from(Map.of(
                CompanionEnv.ADDRESS, "127.0.0.1:41234",
                CompanionEnv.TOKEN, token,
                CompanionEnv.STATS_DIR, "relative/dir"));
        assertNotNull(relative);
        assertNull(relative.statsDir);
    }

    @Test
    void protocolSanitizesToastsAndHost() {
        List<Protocol.ToastItem> toasts = Protocol.toasts(json("""
                {"t":"toasts","items":[
                  {"id":"gameInvite:1","kind":"invite","title":"Kituk","body":"line\\nbreak","count":1,"actionable":true},
                  {"id":"","title":"x"},
                  {"title":"no id"},
                  {"id":"dm:2","title":"Alex","count":3}
                ]}"""));
        assertEquals(2, toasts.size());
        assertEquals("line break", toasts.get(0).body());
        assertTrue(toasts.get(0).actionable());
        assertEquals("message", toasts.get(1).kind());
        assertEquals(3, toasts.get(1).count());

        Protocol.HostState host = Protocol.host(json("""
                {"t":"host","active":true,"kind":"server","guests":[{"key":"user:1","name":"Steve","transport":"direct"},{"name":"nokey"}]}"""));
        assertTrue(host.active());
        assertEquals("server", host.kind());
        assertEquals(1, host.guests().size());
        assertSame(Protocol.HostState.NONE, Protocol.host(json("{\"t\":\"host\",\"active\":false}")));

        assertNull(Protocol.connect(json("{\"t\":\"connect\"}")));
        assertEquals("127.0.0.1:5000", Protocol.connect(json("{\"t\":\"connect\",\"address\":\"127.0.0.1:5000\"}")).name());
    }

    @Test
    void stateShowsEachToastOnceAndAgainWhenCountGrows() {
        CompanionState state = new CompanionState();
        state.setConnected(true);
        state.apply(json("{\"t\":\"toasts\",\"items\":[{\"id\":\"a\",\"title\":\"A\",\"count\":1,\"actionable\":true}]}"));
        assertEquals(1, state.drainFresh().size());
        state.apply(json("{\"t\":\"toasts\",\"items\":[{\"id\":\"a\",\"title\":\"A\",\"count\":1,\"actionable\":true}]}"));
        assertTrue(state.drainFresh().isEmpty());
        state.apply(json("{\"t\":\"toasts\",\"items\":[{\"id\":\"a\",\"title\":\"A\",\"count\":2,\"actionable\":true}]}"));
        assertEquals(1, state.drainFresh().size());
        assertEquals(1, state.view().invites().size());

        state.forgetInvite("a");
        assertTrue(state.view().invites().isEmpty());

        state.apply(json("{\"t\":\"connect\",\"address\":\"127.0.0.1:5000\",\"name\":\"World\"}"));
        assertEquals("World", state.takeConnect().name());
        assertNull(state.takeConnect());

        state.apply(json("{\"t\":\"host\",\"active\":true,\"guests\":[]}"));
        assertTrue(state.view().host().active());
        state.setConnected(false);
        assertFalse(state.view().host().active());
        assertFalse(state.view().connected());
    }

    @Test
    void scheduleRequestsAfterJoinPeriodicallyAndOnPause() {
        StatsSchedule schedule = new StatsSchedule();
        long t = 1_000_000;
        assertEquals(StatsSchedule.Action.NONE, schedule.tick(t, null));
        assertEquals(StatsSchedule.Action.NONE, schedule.tick(t, "mc.example.com"));
        assertEquals(StatsSchedule.Action.NONE, schedule.tick(t + StatsSchedule.FIRST_REQUEST_MS - 1, "mc.example.com"));
        t += StatsSchedule.FIRST_REQUEST_MS;
        assertEquals(StatsSchedule.Action.REQUEST, schedule.tick(t, "mc.example.com"));
        assertEquals(StatsSchedule.Action.NONE, schedule.tick(t + 1_000, "mc.example.com"));
        assertEquals(StatsSchedule.Action.CAPTURE, schedule.tick(t + StatsSchedule.RESPONSE_DELAY_MS, "mc.example.com"));

        schedule.onPause();
        assertEquals(StatsSchedule.Action.NONE, schedule.tick(t + 5_000, "mc.example.com"));
        assertEquals(StatsSchedule.Action.REQUEST, schedule.tick(t + StatsSchedule.PAUSE_GAP_MS, "mc.example.com"));

        long base = t + StatsSchedule.PAUSE_GAP_MS;
        assertEquals(StatsSchedule.Action.CAPTURE, schedule.tick(base + StatsSchedule.RESPONSE_DELAY_MS, "mc.example.com"));
        assertEquals(StatsSchedule.Action.CAPTURE, schedule.tick(base + StatsSchedule.RESPONSE_DELAY_MS + StatsSchedule.REBUILD_MS, "mc.example.com"));
        assertEquals(StatsSchedule.Action.REQUEST, schedule.tick(base + StatsSchedule.PERIOD_MS, "mc.example.com"));

        assertEquals(StatsSchedule.Action.NONE, schedule.tick(base + StatsSchedule.PERIOD_MS + 10, null));
        schedule.onPause();
        assertEquals(StatsSchedule.Action.NONE, schedule.tick(base + StatsSchedule.PERIOD_MS * 3, null));
    }

    @Test
    void snapshotNormalizesAndWritesOnlyChanges(@TempDir Path dir) throws Exception {
        assertEquals("mc.example.com", ServerSnapshot.normalizeAddress(" MC.Example.com:25565 "));
        assertEquals("mc.example.com:25566", ServerSnapshot.normalizeAddress("mc.example.com:25566"));
        assertEquals("mc.example.com", ServerSnapshot.normalizeAddress("mc.example.com."));
        assertNull(ServerSnapshot.normalizeAddress("  "));

        ServerSnapshot writer = new ServerSnapshot();
        JsonObject first = ServerSnapshot.build(
                "mc.example.com",
                "Example",
                new ServerSnapshot.Player("1b2c3d4e-0000-0000-0000-000000000001", "Steve"),
                "1.21.1",
                Map.of("minecraft:mined", Map.of("minecraft:stone", 12, "minecraft:dirt", 0)),
                Map.of("minecraft:story/root", new ServerSnapshot.Advancement(true, List.of("crafting_table"))),
                Instant.parse("2026-11-20T10:00:00Z"));
        assertTrue(writer.write(dir, first));

        Path file = dir.resolve(ServerSnapshot.fileName("mc.example.com", "1b2c3d4e-0000-0000-0000-000000000001"));
        JsonObject saved = json(Files.readString(file));
        assertEquals(12, saved.getAsJsonObject("stats").getAsJsonObject("minecraft:mined").get("minecraft:stone").getAsInt());
        assertFalse(saved.getAsJsonObject("stats").getAsJsonObject("minecraft:mined").has("minecraft:dirt"));
        assertTrue(saved.getAsJsonObject("advancements").getAsJsonObject("minecraft:story/root").get("done").getAsBoolean());

        JsonObject later = first.deepCopy();
        later.addProperty("time", "2026-11-20T10:05:00Z");
        assertFalse(writer.write(dir, later));

        JsonObject changed = ServerSnapshot.build(
                "mc.example.com",
                "Example",
                new ServerSnapshot.Player("1b2c3d4e-0000-0000-0000-000000000001", "Steve"),
                "1.21.1",
                Map.of("minecraft:mined", Map.of("minecraft:stone", 13)),
                Map.of(),
                Instant.parse("2026-11-20T10:06:00Z"));
        assertTrue(writer.write(dir, changed));
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void readLineStopsAtLimit() throws Exception {
        assertEquals("abc", Bridge.readLine(new StringReader("abc\r\nrest"), 10));
        assertNull(Bridge.readLine(new StringReader(""), 10));
        assertThrows(java.io.IOException.class, () -> Bridge.readLine(new StringReader("0123456789x\n"), 10));
    }

    @Test
    void bridgeSendsHelloWaitsForWelcomeAndReconnects() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 4, InetAddress.getLoopbackAddress())) {
            List<String> received = new CopyOnWriteArrayList<>();
            List<Boolean> status = new CopyOnWriteArrayList<>();
            CountDownLatch gotMessage = new CountDownLatch(1);
            CountDownLatch reconnected = new CountDownLatch(2);

            Bridge bridge = new Bridge("127.0.0.1", server.getLocalPort(),
                    Protocol.hello("0123456789abcdef", "1.0.0", "1.21.1", "fabric"),
                    new Bridge.Handler() {
                        @Override
                        public void onMessage(JsonObject message) {
                            received.add(Protocol.type(message));
                            gotMessage.countDown();
                        }

                        @Override
                        public void onConnected(boolean connected) {
                            status.add(connected);
                            if (connected) reconnected.countDown();
                        }
                    });
            bridge.start();

            try (Socket client = server.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                JsonObject hello = json(reader.readLine());
                assertEquals("hello", Protocol.type(hello));
                assertEquals("0123456789abcdef", hello.get("token").getAsString());
                OutputStream out = client.getOutputStream();
                out.write("{\"t\":\"toasts\",\"items\":[]}\n".getBytes(StandardCharsets.UTF_8));
                out.write("{\"t\":\"welcome\"}\n{\"t\":\"host\",\"active\":false}\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                assertTrue(gotMessage.await(5, TimeUnit.SECONDS));
                assertEquals(List.of("host"), received);
                assertTrue(bridge.send(Protocol.kick("user:1")));
                assertEquals("kick", Protocol.type(json(reader.readLine())));
            }

            try (Socket again = server.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(again.getInputStream(), StandardCharsets.UTF_8));
                assertEquals("hello", Protocol.type(json(reader.readLine())));
                again.getOutputStream().write("{\"t\":\"welcome\"}\n".getBytes(StandardCharsets.UTF_8));
                again.getOutputStream().flush();
                assertTrue(reconnected.await(10, TimeUnit.SECONDS));
                assertTrue(bridge.isConnected());
            } finally {
                bridge.close();
            }
            assertEquals(List.of(true, false, true), status.subList(0, 3));
        }
    }
}

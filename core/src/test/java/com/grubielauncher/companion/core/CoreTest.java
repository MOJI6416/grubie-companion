package com.grubielauncher.companion.core;

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
    private static Json.Obj json(String text) {
        Json.Obj obj = Json.parseObject(text);
        assertNotNull(obj, text);
        return obj;
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
                  {"id":"gameInvite:1","kind":"invite","title":"Alex","body":"line\\nbreak","count":1,"actionable":true},
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
        assertFalse(state.place().world());

        state.apply(json("{\"t\":\"place\",\"address\":\"abcd.join.grubielauncher.com\",\"name\":\"kit's world\",\"kind\":\"world\"}"));
        assertTrue(state.place().world());
        assertEquals("kit's world", state.place().name());
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
        Json.Obj first = ServerSnapshot.build(
                "mc.example.com",
                "Example",
                new ServerSnapshot.Player("1b2c3d4e-0000-0000-0000-000000000001", "Steve"),
                "1.21.1",
                Map.of("minecraft:mined", Map.of("minecraft:stone", 12, "minecraft:dirt", 0)),
                Map.of("minecraft:story/root", new ServerSnapshot.Advancement(true, List.of("crafting_table"))),
                Instant.parse("2026-11-20T10:00:00Z"));
        assertTrue(writer.write(dir, first));

        Path file = dir.resolve(ServerSnapshot.fileName("mc.example.com", "1b2c3d4e-0000-0000-0000-000000000001"));
        Json.Obj saved = json(Files.readString(file));
        assertEquals(12, saved.object("stats").object("minecraft:mined").integer("minecraft:stone", -1));
        assertFalse(saved.object("stats").object("minecraft:mined").has("minecraft:dirt"));
        assertTrue(saved.object("advancements").object("minecraft:story/root").bool("done"));

        Json.Obj later = json(first.toString());
        later.put("time", "2026-11-20T10:05:00Z");
        assertFalse(writer.write(dir, later));

        Json.Obj changed = ServerSnapshot.build(
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
            CountDownLatch gotMessage = new CountDownLatch(2);
            CountDownLatch reconnected = new CountDownLatch(2);

            Bridge bridge = new Bridge("127.0.0.1", server.getLocalPort(),
                    Protocol.hello("0123456789abcdef", "2.0.0", "1.21.1", "fabric", List.of("panel")),
                    new Bridge.Handler() {
                        @Override
                        public void onMessage(Json.Obj message) {
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
                Json.Obj hello = json(reader.readLine());
                assertEquals("hello", Protocol.type(hello));
                assertEquals("0123456789abcdef", hello.string("token", 64));
                assertEquals(2, hello.integer("protocol", 0));
                OutputStream out = client.getOutputStream();
                out.write("{\"t\":\"toasts\",\"items\":[]}\n".getBytes(StandardCharsets.UTF_8));
                out.write("{\"t\":\"welcome\"}\n{\"t\":\"host\",\"active\":false}\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                assertTrue(gotMessage.await(5, TimeUnit.SECONDS));
                assertEquals(List.of("welcome", "host"), received);
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
            assertEquals(List.of(true, false, true), List.copyOf(status).subList(0, 3));
        }
    }

    @Test
    void jsonParsesWritesAndRejectsGarbage() {
        Json.Obj obj = json("{\"a\":[1,2.5,\"x\u0041\n\"],\"b\":{\"c\":true,\"d\":null},\"e\":-3e2}");
        assertEquals(-300, obj.integer("e", 0));
        assertTrue(obj.object("b").bool("c"));
        assertEquals("{\"a\":[1,2.5,\"xA\\n\"],\"b\":{\"c\":true,\"d\":null},\"e\":-300}", obj.toString());
        assertNull(Json.parseObject("[1,2]"));
        assertNull(Json.parseObject("{\"a\":1} trailing"));
        assertNull(Json.parseObject("{\"a\":"));
        assertNull(Json.parseObject("[".repeat(100)));
        assertEquals("\"\\u2028\"", Json.write(String.valueOf((char) 0x2028)));
    }

    @Test
    void protocolReadsV2State() {
        Protocol.FriendsState friends = Protocol.friends(json("""
                {"t":"friends","online":[{"id":"u1","name":"Alex","status":"playing","activity":"Остров","joinable":true},{"name":"no id"}],"offline":12}"""));
        assertEquals(1, friends.online().size());
        assertTrue(friends.online().get(0).joinable());
        assertEquals(12, friends.offline());
        assertTrue(friends.offlineList().isEmpty());
        Protocol.FriendsState withOffline = Protocol.friends(json("{\"t\":\"friends\",\"online\":[],\"offline\":2,\"offlineList\":[{\"id\":\"u3\",\"name\":\"Pacyk\",\"avatar\":\"u3\"},{\"name\":\"no id\"}]}"));
        assertEquals(1, withOffline.offlineList().size());
        assertEquals("Pacyk", withOffline.offlineList().get(0).name());

        Protocol.VoiceState voice = Protocol.voice(json("""
                {"t":"voice","active":true,"channel":"Builders","muted":true,"ptt":true,"pttKey":"V","members":[{"id":"u1","name":"Alex","speaking":true}]}"""));
        assertTrue(voice.active());
        assertTrue(voice.muted());
        assertEquals("V", voice.pttKey());
        assertTrue(voice.members().get(0).speaking());
        assertSame(Protocol.VoiceState.NONE, Protocol.voice(json("{\"t\":\"voice\"}")));

        Protocol.HostState host = Protocol.host(json("""
                {"t":"host","active":true,"title":"Island","code":"ABCD-EFGH","max":8,"guests":[{"key":"user:1","name":"Steve","transport":"direct","ping":32}],"invited":["Alex"]}"""));
        assertEquals(32, host.guests().get(0).ping());
        assertEquals(List.of("Alex"), host.invited());
        assertEquals("ABCD-EFGH", host.code());

        assertNull(Protocol.avatar(json("{\"t\":\"avatar\",\"id\":\"u1\",\"png\":\"bm90IGEgcG5n\"}")));
        Protocol.Avatar avatar = Protocol.avatar(json("{\"t\":\"avatar\",\"id\":\"u1\",\"png\":\"iVBORw0KGgoAAAA=\"}"));
        assertNotNull(avatar);
        assertEquals("u1", avatar.id());

        Protocol.Result result = Protocol.result(json("{\"t\":\"result\",\"req\":\"r1\",\"ok\":false,\"error\":\"offline\"}"));
        assertEquals("offline", result.error());
        assertEquals(2, Protocol.welcome(json("{\"t\":\"welcome\",\"protocol\":2,\"selfName\":\"Steve\"}")).protocol());
        assertEquals(1, Protocol.welcome(json("{\"t\":\"welcome\"}")).protocol());
    }

    @Test
    void stateQueuesAvatarsAndResultsAndClearsOnDisconnect() {
        CompanionState state = new CompanionState();
        state.setConnected(true);
        state.apply(json("{\"t\":\"welcome\",\"protocol\":2,\"selfName\":\"Steve\"}"));
        assertEquals(2, state.view().protocol());
        assertEquals("Steve", state.view().selfName());
        state.apply(json("{\"t\":\"avatar\",\"id\":\"u1\",\"png\":\"iVBORw0KGgoAAAA=\"}"));
        state.apply(json("{\"t\":\"result\",\"req\":\"r1\",\"ok\":true}"));
        assertEquals("u1", state.takeAvatar().id());
        assertNull(state.takeAvatar());
        assertTrue(state.takeResult().ok());
        state.apply(json("{\"t\":\"friends\",\"online\":[{\"id\":\"u1\",\"name\":\"A\"}],\"offline\":1}"));
        assertEquals(1, state.view().friends().online().size());
        state.setConnected(false);
        assertTrue(state.view().friends().online().isEmpty());
        assertEquals(0, state.view().protocol());
    }
}

package com.grubielauncher.companion.core;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public final class Protocol {
    public static final int VERSION = 2;
    static final int MAX_TOASTS = 8;
    static final int MAX_GUESTS = 64;
    static final int MAX_FRIENDS = 200;
    static final int MAX_MEMBERS = 64;
    static final int MAX_TEXT = 300;
    static final int MAX_AVATAR_BYTES = 64 * 1024;

    private Protocol() {
    }

    public record ToastItem(String id, String kind, String title, String body, String meta, int count, boolean actionable, String avatar) {
    }

    public record Friend(String id, String name, String status, String activity, String avatar, boolean joinable) {
    }

    public record FriendsState(List<Friend> online, int offline, List<Friend> offlineList) {
        public static final FriendsState NONE = new FriendsState(List.of(), 0, List.of());
    }

    public record Guest(String key, String name, String transport, int ping, String avatar) {
    }

    public record HostState(boolean active, String kind, String title, String code, String visibility, int max, List<Guest> guests, List<String> invited) {
        public static final HostState NONE = new HostState(false, "", null, null, null, 0, List.of(), List.of());
    }

    public record VoiceMember(String id, String name, boolean speaking, boolean muted, String avatar) {
    }

    public record VoiceState(boolean active, String channel, boolean muted, boolean deafened, boolean ptt, String pttKey, int pttCode, List<VoiceMember> members) {
        public static final VoiceState NONE = new VoiceState(false, null, false, false, false, null, -1, List.of());
    }

    public record ConnectRequest(String address, String name, String kind) {
        public boolean world() {
            return "world".equals(kind);
        }
    }

    public record Welcome(int protocol, String locale, String selfId, String selfName) {
    }

    public record Result(String req, boolean ok, String error) {
    }

    public record Avatar(String id, byte[] png) {
    }

    public static Json.Obj hello(String token, String modVersion, String minecraft, String loader, List<String> features) {
        return message("hello")
                .put("protocol", VERSION)
                .put("token", token)
                .put("mod", modVersion)
                .put("minecraft", minecraft)
                .put("loader", loader)
                .put("features", new ArrayList<>(features));
    }

    public static Json.Obj accept(String toastId) {
        return message("accept").put("id", toastId);
    }

    public static Json.Obj decline(String toastId) {
        return message("decline").put("id", toastId);
    }

    public static Json.Obj kick(String guestKey) {
        return message("kick").put("key", guestKey);
    }

    public static Json.Obj inviteToServer(String req, String friendId, String address, String name) {
        return message("invite").put("req", req).put("friend", friendId)
                .put("server", new Json.Obj().put("address", address).put("name", name));
    }

    public static Json.Obj join(String req, String friendId) {
        return message("join").put("req", req).put("friend", friendId);
    }

    public static Json.Obj inviteToWorld(String req, String friendId) {
        return message("invite").put("req", req).put("friend", friendId);
    }

    public static Json.Obj shareStart(String req, int port, String visibility, String worldName) {
        return message("share").put("req", req).put("action", "start").put("port", port)
                .put("visibility", visibility).put("world", worldName);
    }

    public static Json.Obj shareStop(String req) {
        return message("share").put("req", req).put("action", "stop");
    }

    public static Json.Obj voice(String action) {
        return message("voice").put("action", action);
    }

    public static Json.Obj ptt(boolean down) {
        return message("ptt").put("down", down);
    }

    public static Json.Obj screenshot(String path) {
        return message("screenshot").put("path", path);
    }

    public static Json.Obj sendScreenshot(String req, String path, String friendId) {
        return message("sendScreenshot").put("req", req).put("path", path).put("friend", friendId);
    }

    public static Json.Obj stage(String stage, String detail, int progress) {
        Json.Obj message = message("stage").put("stage", stage).put("detail", detail);
        if (progress >= 0) message.put("progress", progress);
        return message;
    }

    public static Json.Obj hang(long seconds, String thread, List<String> frames, List<String> mods) {
        return message("hang").put("seconds", seconds).put("thread", thread)
                .put("frames", new ArrayList<>(frames)).put("mods", new ArrayList<>(mods));
    }

    public static Json.Obj stats(String kind, String key, String name, Json.Obj snapshot) {
        return message("stats").put("kind", kind).put("key", key).put("name", name).put("snapshot", snapshot);
    }

    public static String type(Json.Obj message) {
        return message.string("t", 32);
    }

    public static Welcome welcome(Json.Obj message) {
        return new Welcome(
                Math.max(1, message.integer("protocol", 1)),
                message.string("locale", 16),
                message.string("selfId", 64),
                message.string("selfName", 64));
    }

    public static List<ToastItem> toasts(Json.Obj message) {
        List<ToastItem> result = new ArrayList<>();
        for (Json.Obj item : message.objects("items", 64)) {
            if (result.size() >= MAX_TOASTS) break;
            String id = item.string("id", 200);
            String title = item.string("title", MAX_TEXT);
            if (id == null || title == null) continue;
            String kind = item.string("kind", 16);
            String body = item.string("body", MAX_TEXT);
            result.add(new ToastItem(
                    id,
                    kind == null ? "message" : kind,
                    title,
                    body == null ? "" : body,
                    item.string("meta", 120),
                    Math.max(1, item.integer("count", 1)),
                    item.bool("actionable"),
                    item.string("avatar", 128)));
        }
        return Collections.unmodifiableList(result);
    }

    public static FriendsState friends(Json.Obj message) {
        List<Friend> online = new ArrayList<>();
        for (Json.Obj item : message.objects("online", MAX_FRIENDS)) {
            String id = item.string("id", 64);
            String name = item.string("name", 64);
            if (id == null || name == null) continue;
            String status = item.string("status", 16);
            online.add(new Friend(id, name, status == null ? "online" : status, item.string("activity", 120), item.string("avatar", 128), item.bool("joinable")));
        }
        List<Friend> offlineList = new ArrayList<>();
        for (Json.Obj item : message.objects("offlineList", MAX_FRIENDS)) {
            String id = item.string("id", 64);
            String name = item.string("name", 64);
            if (id == null || name == null) continue;
            offlineList.add(new Friend(id, name, "offline", null, item.string("avatar", 128), false));
        }
        return new FriendsState(Collections.unmodifiableList(online), Math.max(0, message.integer("offline", 0)), Collections.unmodifiableList(offlineList));
    }

    public static HostState host(Json.Obj message) {
        if (!message.bool("active")) return HostState.NONE;
        List<Guest> guests = new ArrayList<>();
        for (Json.Obj item : message.objects("guests", MAX_GUESTS)) {
            String key = item.string("key", 200);
            if (key == null) continue;
            String name = item.string("name", 64);
            String transport = item.string("transport", 16);
            guests.add(new Guest(key, name == null ? "?" : name, transport == null ? "relay" : transport, item.integer("ping", -1), item.string("avatar", 128)));
        }
        String kind = message.string("kind", 16);
        return new HostState(
                true,
                kind == null ? "world" : kind,
                message.string("title", 120),
                message.string("code", 32),
                message.string("visibility", 16),
                Math.max(0, message.integer("max", 0)),
                Collections.unmodifiableList(guests),
                message.strings("invited", 32, 64));
    }

    public static VoiceState voice(Json.Obj message) {
        if (!message.bool("active")) return VoiceState.NONE;
        List<VoiceMember> members = new ArrayList<>();
        for (Json.Obj item : message.objects("members", MAX_MEMBERS)) {
            String id = item.string("id", 64);
            String name = item.string("name", 64);
            if (id == null || name == null) continue;
            members.add(new VoiceMember(id, name, item.bool("speaking"), item.bool("muted"), item.string("avatar", 128)));
        }
        return new VoiceState(
                true,
                message.string("channel", 120),
                message.bool("muted"),
                message.bool("deafened"),
                message.bool("ptt"),
                message.string("pttKey", 32),
                message.integer("pttCode", -1),
                Collections.unmodifiableList(members));
    }

    public static ConnectRequest connect(Json.Obj message) {
        String address = message.string("address", 255);
        if (address == null) return null;
        String name = message.string("name", 64);
        String kind = message.string("kind", 16);
        return new ConnectRequest(address, name == null ? address : name, "world".equals(kind) ? "world" : "server");
    }

    public static Result result(Json.Obj message) {
        String req = message.string("req", 64);
        if (req == null) return null;
        return new Result(req, message.bool("ok"), message.string("error", 64));
    }

    public static Avatar avatar(Json.Obj message) {
        String id = message.string("id", 128);
        String png = message.rawString("png", MAX_AVATAR_BYTES * 2);
        if (id == null || png == null) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(png);
            if (bytes.length == 0 || bytes.length > MAX_AVATAR_BYTES) return null;
            if (bytes.length < 8 || (bytes[0] & 0xff) != 0x89 || bytes[1] != 'P' || bytes[2] != 'N' || bytes[3] != 'G') return null;
            return new Avatar(id, bytes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Json.Obj message(String type) {
        return new Json.Obj().put("t", type);
    }
}

import base64
import json
import socket
import struct
import sys
import threading
import time
import zlib

port = int(sys.argv[1])
token = sys.argv[2]
log_path = sys.argv[3]


def head_png():
    width = height = 8
    rows = b""
    for y in range(height):
        row = bytes((0,))
        for x in range(width):
            if y < 3:
                row += bytes((0xD7, 0x77, 0x2A, 0xFF))
            elif y == 4 and x in (2, 5):
                row += bytes((0x2F, 0x7D, 0x43, 0xFF))
            else:
                row += bytes((0xF1, 0xC2, 0x9A, 0xFF))
        rows += row

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = bytes((0x89,)) + b"PNG" + bytes((0x0D, 0x0A, 0x1A, 0x0A))
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(rows))
    png += chunk(b"IEND", b"")
    return base64.b64encode(png).decode("ascii")


HEAD = head_png()


def log(text):
    with open(log_path, "a", encoding="utf-8") as handle:
        handle.write(time.strftime("%H:%M:%S") + " " + text + "\n")


def send(conn, message):
    conn.sendall((json.dumps(message, ensure_ascii=False) + "\n").encode("utf-8"))


def serve(conn):
    reader = conn.makefile("r", encoding="utf-8")
    hello = json.loads(reader.readline())
    summary = {key: hello.get(key) for key in ("mod", "minecraft", "loader", "protocol")}
    log("hello " + json.dumps(summary) + " token_ok=" + str(hello.get("token") == token))
    if hello.get("token") != token:
        conn.close()
        return
    send(conn, {"t": "welcome", "protocol": 2, "selfName": "CiBot", "selfId": "self"})
    send(conn, {"t": "avatar", "id": "u1", "png": HEAD})
    send(conn, {"t": "friends", "online": [
        {"id": "u1", "name": "Alex", "status": "playing", "activity": "Island", "avatar": "u1", "joinable": True},
        {"id": "u2", "name": "Миша", "status": "online"},
    ], "offline": 3})
    send(conn, {"t": "voice", "active": True, "channel": "Builders", "ptt": False, "members": [
        {"id": "u1", "name": "Alex", "speaking": True, "avatar": "u1"},
        {"id": "self", "name": "CiBot", "muted": True},
    ]})
    send(conn, {"t": "toasts", "items": [
        {"id": "dm:u1", "kind": "message", "title": "Alex", "body": "Привет! Заходи к нам", "avatar": "u1"},
        {"id": "gameInvite:u1", "kind": "invite", "title": "Alex", "body": "Зовёт в мир «Остров»", "actionable": True, "avatar": "u1"},
        {"id": "ach:worlds_5", "kind": "achievement", "title": "Путешественник", "body": "5 разных миров", "meta": "+25"},
    ]})
    for line in reader:
        log("game " + line.strip())
    log("game disconnected")


listener = socket.socket()
listener.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
listener.bind(("127.0.0.1", port))
listener.listen(4)
log("listening on " + str(port))
while True:
    connection, _ = listener.accept()
    threading.Thread(target=serve, args=(connection,), daemon=True).start()

package com.grubielauncher.companion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private static final int MAX_DEPTH = 32;

    private Json() {
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        parser.skipWhitespace();
        Object value = parser.value(0);
        parser.skipWhitespace();
        if (parser.pos != text.length()) throw new IllegalArgumentException("Trailing data at " + parser.pos);
        return value;
    }

    public static Obj parseObject(String text) {
        try {
            Object value = parse(text);
            return value instanceof Map ? new Obj(cast(value)) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    private static void write(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof Obj) {
            write(out, ((Obj) value).map);
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) out.append(',');
                first = false;
                quote(out, String.valueOf(entry.getKey()));
                out.append(':');
                write(out, entry.getValue());
            }
            out.append('}');
        } else if (value instanceof List) {
            out.append('[');
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) out.append(',');
                first = false;
                write(out, item);
            }
            out.append(']');
        } else if (value instanceof String) {
            quote(out, (String) value);
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                out.append("null");
            } else if (number == Math.rint(number) && Math.abs(number) < 1e15) {
                out.append((long) number);
            } else {
                out.append(number);
            }
        } else if (value instanceof Number) {
            out.append(((Number) value).longValue());
        } else {
            quote(out, value.toString());
        }
    }

    private static void quote(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20 || c == 0x2028 || c == 0x2029) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    public static final class Obj {
        private final Map<String, Object> map;

        public Obj() {
            this.map = new LinkedHashMap<>();
        }

        Obj(Map<String, Object> map) {
            this.map = map;
        }

        public Obj put(String key, Object value) {
            if (value instanceof Obj) value = ((Obj) value).map;
            if (value != null) map.put(key, value);
            return this;
        }

        public Obj remove(String key) {
            map.remove(key);
            return this;
        }

        public boolean has(String key) {
            return map.containsKey(key);
        }

        public Object raw(String key) {
            return map.get(key);
        }

        public Map<String, Object> map() {
            return map;
        }

        public String string(String key, int limit) {
            Object value = map.get(key);
            if (!(value instanceof String)) return null;
            String text = ((String) value).replaceAll("\\p{Cntrl}", " ").trim();
            if (text.isEmpty()) return null;
            return text.length() > limit ? text.substring(0, limit) : text;
        }

        public String rawString(String key, int limit) {
            Object value = map.get(key);
            if (!(value instanceof String)) return null;
            String text = (String) value;
            return text.length() > limit ? null : text;
        }

        public int integer(String key, int fallback) {
            Object value = map.get(key);
            if (!(value instanceof Number)) return fallback;
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || number > Integer.MAX_VALUE || number < Integer.MIN_VALUE) return fallback;
            return (int) number;
        }

        public double number(String key, double fallback) {
            Object value = map.get(key);
            return value instanceof Number ? ((Number) value).doubleValue() : fallback;
        }

        public boolean bool(String key) {
            return Boolean.TRUE.equals(map.get(key));
        }

        public Obj object(String key) {
            Object value = map.get(key);
            return value instanceof Map ? new Obj(cast(value)) : null;
        }

        public List<Obj> objects(String key, int limit) {
            Object value = map.get(key);
            if (!(value instanceof List)) return Collections.emptyList();
            List<Obj> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (result.size() >= limit) break;
                if (item instanceof Map) result.add(new Obj(cast(item)));
            }
            return result;
        }

        public List<String> strings(String key, int limit, int length) {
            Object value = map.get(key);
            if (!(value instanceof List)) return Collections.emptyList();
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (result.size() >= limit) break;
                if (item instanceof String && ((String) item).length() <= length) result.add((String) item);
            }
            return result;
        }

        @Override
        public String toString() {
            return write(map);
        }
    }

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        void skipWhitespace() {
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') break;
                pos++;
            }
        }

        Object value(int depth) {
            if (depth > MAX_DEPTH) throw new IllegalArgumentException("Too deep");
            if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end");
            char c = text.charAt(pos);
            return switch (c) {
                case '{' -> object(depth);
                case '[' -> array(depth);
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        Map<String, Object> object(int depth) {
            Map<String, Object> result = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') throw new IllegalArgumentException("Expected key at " + pos);
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                result.put(key, value(depth + 1));
                skipWhitespace();
                char c = next();
                if (c == '}') return result;
                if (c != ',') throw new IllegalArgumentException("Expected , or } at " + pos);
            }
        }

        List<Object> array(int depth) {
            List<Object> result = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                result.add(value(depth + 1));
                skipWhitespace();
                char c = next();
                if (c == ']') return result;
                if (c != ',') throw new IllegalArgumentException("Expected , or ] at " + pos);
            }
        }

        String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return out.toString();
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                char escape = next();
                switch (escape) {
                    case '"', '\\', '/' -> out.append(escape);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (pos + 4 > text.length()) throw new IllegalArgumentException("Bad escape");
                        out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw new IllegalArgumentException("Bad escape at " + pos);
                }
            }
        }

        Object number() {
            int start = pos;
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    pos++;
                } else {
                    break;
                }
            }
            if (start == pos) throw new IllegalArgumentException("Unexpected character at " + pos);
            return Double.parseDouble(text.substring(start, pos));
        }

        Object literal(String word, Object value) {
            if (!text.startsWith(word, pos)) throw new IllegalArgumentException("Unexpected token at " + pos);
            pos += word.length();
            return value;
        }

        char peek() {
            if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end");
            return text.charAt(pos);
        }

        char next() {
            if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end");
            return text.charAt(pos++);
        }

        void expect(char c) {
            if (next() != c) throw new IllegalArgumentException("Expected " + c + " at " + (pos - 1));
        }
    }
}

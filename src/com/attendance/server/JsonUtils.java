package com.attendance.server;

import java.lang.reflect.Method;
import java.util.*;

public class JsonUtils {

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return quote((String) obj);
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append(quote(String.valueOf(entry.getKey()))).append(":").append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection<?>) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : arr) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }

        // Generic POJO serialization using getters
        Map<String, Object> map = new LinkedHashMap<>();
        for (Method m : obj.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && !m.getName().equals("getClass")) {
                String name = m.getName();
                String propName = null;
                if (name.startsWith("get") && name.length() > 3) {
                    propName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                } else if (name.startsWith("is") && name.length() > 2) {
                    propName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }
                if (propName != null) {
                    try {
                        Object val = m.invoke(obj);
                        map.put(propName, val);
                    } catch (Exception ignored) {}
                }
            }
        }
        return toJson(map);
    }

    private static String quote(String string) {
        if (string == null || string.isEmpty()) return "\"\"";
        char c;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        sb.append('"');
        for (int i = 0; i < len; i += 1) {
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\').append(c);
                    break;
                case '\b': sb.append("\\b"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\f': sb.append("\\f"); break;
                case '\r': sb.append("\\r"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // Simple JSON Parser
    public static Object parse(String json) {
        if (json == null) return null;
        json = json.trim();
        if (json.isEmpty()) return null;
        int[] index = new int[]{0};
        return parseValue(json, index);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object res = parse(json);
        if (res instanceof Map) {
            return (Map<String, Object>) res;
        }
        return new HashMap<>();
    }

    private static Object parseValue(String s, int[] idx) {
        skipWhitespace(s, idx);
        if (idx[0] >= s.length()) return null;
        char c = s.charAt(idx[0]);
        if (c == '{') return parseMap(s, idx);
        if (c == '[') return parseList(s, idx);
        if (c == '"') return parseString(s, idx);
        if (c == 't' || c == 'f') return parseBoolean(s, idx);
        if (c == 'n') return parseNull(s, idx);
        return parseNumber(s, idx);
    }

    private static void skipWhitespace(String s, int[] idx) {
        while (idx[0] < s.length() && Character.isWhitespace(s.charAt(idx[0]))) {
            idx[0]++;
        }
    }

    private static Map<String, Object> parseMap(String s, int[] idx) {
        Map<String, Object> map = new LinkedHashMap<>();
        idx[0]++; // Skip '{'
        skipWhitespace(s, idx);
        if (idx[0] < s.length() && s.charAt(idx[0]) == '}') {
            idx[0]++;
            return map;
        }
        while (idx[0] < s.length()) {
            skipWhitespace(s, idx);
            String key = parseString(s, idx);
            skipWhitespace(s, idx);
            if (idx[0] < s.length() && s.charAt(idx[0]) == ':') {
                idx[0]++;
            }
            Object val = parseValue(s, idx);
            map.put(key, val);
            skipWhitespace(s, idx);
            if (idx[0] < s.length() && s.charAt(idx[0]) == ',') {
                idx[0]++;
            } else if (idx[0] < s.length() && s.charAt(idx[0]) == '}') {
                idx[0]++;
                break;
            }
        }
        return map;
    }

    private static List<Object> parseList(String s, int[] idx) {
        List<Object> list = new ArrayList<>();
        idx[0]++; // Skip '['
        skipWhitespace(s, idx);
        if (idx[0] < s.length() && s.charAt(idx[0]) == ']') {
            idx[0]++;
            return list;
        }
        while (idx[0] < s.length()) {
            Object val = parseValue(s, idx);
            list.add(val);
            skipWhitespace(s, idx);
            if (idx[0] < s.length() && s.charAt(idx[0]) == ',') {
                idx[0]++;
            } else if (idx[0] < s.length() && s.charAt(idx[0]) == ']') {
                idx[0]++;
                break;
            }
        }
        return list;
    }

    private static String parseString(String s, int[] idx) {
        idx[0]++; // Skip opening quote
        StringBuilder sb = new StringBuilder();
        while (idx[0] < s.length()) {
            char c = s.charAt(idx[0]++);
            if (c == '"') {
                return sb.toString();
            } else if (c == '\\' && idx[0] < s.length()) {
                char next = s.charAt(idx[0]++);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (idx[0] + 4 <= s.length()) {
                            String hex = s.substring(idx[0], idx[0] + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            idx[0] += 4;
                        }
                        break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Boolean parseBoolean(String s, int[] idx) {
        if (s.startsWith("true", idx[0])) {
            idx[0] += 4;
            return Boolean.TRUE;
        } else if (s.startsWith("false", idx[0])) {
            idx[0] += 5;
            return Boolean.FALSE;
        }
        return null;
    }

    private static Object parseNull(String s, int[] idx) {
        if (s.startsWith("null", idx[0])) {
            idx[0] += 4;
        }
        return null;
    }

    private static Number parseNumber(String s, int[] idx) {
        int start = idx[0];
        boolean isDouble = false;
        while (idx[0] < s.length()) {
            char c = s.charAt(idx[0]);
            if (c == '.' || c == 'e' || c == 'E') {
                isDouble = true;
                idx[0]++;
            } else if (Character.isDigit(c) || c == '-' || c == '+') {
                idx[0]++;
            } else {
                break;
            }
        }
        String numStr = s.substring(start, idx[0]);
        try {
            if (isDouble) return Double.parseDouble(numStr);
            long l = Long.parseLong(numStr);
            if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) return (int) l;
            return l;
        } catch (Exception e) {
            return 0;
        }
    }
}

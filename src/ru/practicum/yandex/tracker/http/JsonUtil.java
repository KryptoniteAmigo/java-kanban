package ru.practicum.yandex.tracker.http;

import com.google.gson.*;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (v, t, c) ->
                    v == null ? JsonNull.INSTANCE : new JsonPrimitive(v.toString()))
            .registerTypeAdapter(Duration.class, (JsonSerializer<Duration>) (v, t, c) ->
                    v == null ? JsonNull.INSTANCE : new JsonPrimitive(v.toMinutes()))
            .create();

    private static boolean has(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull();
    }

    private static String str(JsonObject o, String... keys) {
        for (String k : keys) {
            if (has(o, k)) {
                return o.get(k).getAsString();
            }
        }
        return "";
    }

    private static Integer num(JsonObject o, String k) {
        return has(o, k) ? o.get(k).getAsInt() : null;
    }

    private static Long numL(JsonObject o, String... keys) {
        for (String k : keys) {
            if (!has(o, k)) continue;
            try {
                return o.get(k).getAsLong();
            } catch (Exception ignore) {
                try {
                    String s = o.get(k).getAsString();
                    return java.time.Duration.parse(s).toMinutes();
                } catch (Exception ignore2) {
                }
            }
        }
        return null;
    }

    private static LocalDateTime time(JsonObject o, String... keys) {
        String s = str(o, keys);
        return s.isEmpty() ? null : LocalDateTime.parse(s);
    }

    public static Epic parseEpic(String json) {
        try {
            JsonObject j = JsonParser.parseString(json).getAsJsonObject();
            int id = num(j, "id") == null ? 0 : num(j, "id");
            String title = str(j, "title");
            String desc = str(j, "description", "desc");
            return new Epic(id, title, desc);
        } catch (Exception e) {
            return null;
        }
    }

    public static Subtask parseSubtask(String json) {
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        int id = num(j, "id") == null ? 0 : num(j, "id");
        String ttl = str(j, "title");
        String dsc = str(j, "description", "desc");
        int epicId = num(j, "epicId") == null ? 0 : num(j, "epicId");

        Subtask s = new Subtask(id, ttl, dsc, epicId);

        java.time.Duration d = duration(j);
        if (d != null) {
            s.setDuration(d);
        }

        LocalDateTime st = time(j, "start", "startTime");
        if (st != null) {
            s.setStartTime(st);
        }
        return s;
    }

    public static Task parseTask(String json) {
        JsonObject j = JsonParser.parseString(json).getAsJsonObject();
        int id = num(j, "id") == null ? 0 : num(j, "id");
        String title = str(j, "title");
        String desc = str(j, "description", "desc");

        Task t = new Task(id, title, desc);

        java.time.Duration d = duration(j);
        if (d != null) {
            t.setDuration(d);
        }

        LocalDateTime st = time(j, "start", "startTime");
        if (st != null) {
            t.setStartTime(st);
        }
        return t;
    }

    public static Gson gson() {
        return GSON;
    }

    private static Duration duration(JsonObject o) {
        Long mins = numL(o, "durationMinutes", "duration", "dur");
        return mins == null ? null : java.time.Duration.ofMinutes(mins);
    }
}
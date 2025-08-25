package ru.practicum.yandex.tracker.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BaseHttpHandler {

    protected static String readBody(HttpExchange h) throws IOException {
        try (InputStream is = h.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected static void send(HttpExchange h, int code, String text, String contentType) throws IOException {
        byte[] resp = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        if (contentType != null) {
            h.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        }
        h.sendResponseHeaders(code, resp.length);
        if (resp.length > 0) {
            h.getResponseBody().write(resp);
        }
        h.close();
    }

    protected static void sendJson(HttpExchange h, int code, String json) throws IOException {
        send(h, code, json, "application/json");
    }

    protected static void sendText(HttpExchange h, int code, String text) throws IOException {
        send(h, code, text, "text/plain");
    }

    protected static void notFound(HttpExchange h) throws IOException {
        sendText(h, 404, "Not found");
    }

    protected static void conflict406(HttpExchange h, String msg) throws IOException {
        sendText(h, 406, msg == null ? "Not acceptable" : msg);
    }

    protected static void serverError(HttpExchange h, Throwable ex) throws IOException {
        ex.printStackTrace();
        sendText(h, 500, "Server error: " + ex.getClass().getSimpleName() +
                (ex.getMessage() == null ? "" : (": " + ex.getMessage())));
    }

    protected static Integer parseId(String path, String base) {
        if (!path.startsWith(base)) {
            return null;
        }
        String tail = path.substring(base.length());
        if (tail.isEmpty() || "/".equals(tail)) {
            return null;
        }
        if (tail.startsWith("/")) {
            tail = tail.substring(1);
        }
        if (tail.isEmpty()) {
            return null;
        }
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

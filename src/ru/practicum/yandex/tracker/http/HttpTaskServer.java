package ru.practicum.yandex.tracker.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.yandex.tracker.manager.Managers;
import ru.practicum.yandex.tracker.manager.TaskManager;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static ru.practicum.yandex.tracker.http.BaseHttpHandler.*;

public class HttpTaskServer {

    private final HttpServer server;
    private final TaskManager manager;
    private final Gson gson = JsonUtil.gson();

    public HttpTaskServer(TaskManager manager) throws IOException {
        this.manager = manager;
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/tasks", this::handleTasks);
        server.createContext("/subtasks", this::handleSubtasks);
        server.createContext("/epics", this::handleEpics);
        server.createContext("/history", this::handleHistory);
        server.createContext("/prioritized", this::handlePrioritized);
    }

    public static void main(String[] args) throws IOException {
        TaskManager tm = Managers.getDefault();
        new HttpTaskServer(tm).start();
    }

    public void start() {
        server.start();
        System.out.println("HTTP server started on http://localhost:8080");
    }

    public void stop() {
        server.stop(0);
    }

    private void handleTasks(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            String path = h.getRequestURI().getPath();

            if ("GET".equals(method)) {
                Integer id = parseId(path, "/tasks");
                if (id == null) {
                    List<Task> all = manager.getAllTasks();
                    sendJson(h, 200, gson.toJson(all));
                } else {
                    Task t = manager.getTaskById(id);
                    if (t == null) {
                        notFound(h);
                        return;
                    }
                    sendJson(h, 200, gson.toJson(t));
                }
                return;
            }

            if ("POST".equals(method)) {
                String body = readBody(h);
                Task incoming = JsonUtil.parseTask(body);
                if (incoming == null) {
                    sendText(h, 400, "Empty body");
                    return;
                }

                try {
                    if (incoming.getId() <= 0) {
                        int id = manager.createTask(incoming);
                        Task created = manager.getTaskById(id);
                        sendJson(h, 201, gson.toJson(created));
                    } else {
                        if (manager.getTaskById(incoming.getId()) == null) {
                            notFound(h);
                            return;
                        }
                        manager.updateTask(incoming);
                        sendJson(h, 200, gson.toJson(manager.getTaskById(incoming.getId())));
                    }
                } catch (IllegalArgumentException | IllegalStateException overlap) {
                    conflict406(h, overlap.getMessage());
                }
                return;
            }

            if ("DELETE".equals(method)) {
                Integer id = parseId(path, "/tasks");
                if (id == null) {
                    sendText(h, 400, "id required");
                    return;
                }
                Task existed = manager.getTaskById(id);
                if (existed == null) {
                    notFound(h);
                    return;
                }
                manager.deleteTaskById(id);
                sendText(h, 200, "deleted");
                return;
            }

            sendText(h, 405, "Method Not Allowed");
        } catch (Throwable ex) {
            serverError(h, ex);
        }
    }

    private void handleSubtasks(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            String path = h.getRequestURI().getPath();

            if ("GET".equals(method)) {
                Integer id = parseId(path, "/subtasks");
                if (id == null) {
                    sendJson(h, 200, gson.toJson(manager.getAllSubtasks()));
                } else {
                    Subtask s = manager.getSubtaskById(id);
                    if (s == null) {
                        notFound(h);
                        return;
                    }
                    sendJson(h, 200, gson.toJson(s));
                }
                return;
            }

            if ("POST".equals(method)) {
                String body = readBody(h);
                Subtask incoming = JsonUtil.parseSubtask(body);
                if (incoming == null) {
                    sendText(h, 400, "Empty body");
                    return;
                }
                if (incoming.getId() <= 0 &&
                        (incoming.getEpicId() <= 0 || manager.getEpicById(incoming.getEpicId()) == null)) {
                    notFound(h);
                    return;
                }

                try {
                    if (incoming.getId() <= 0) {
                        int id = manager.createSubtask(incoming);
                        sendJson(h, 201, gson.toJson(manager.getSubtaskById(id)));
                    } else {
                        if (manager.getSubtaskById(incoming.getId()) == null) {
                            notFound(h);
                            return;
                        }
                        manager.updateSubtask(incoming);
                        sendJson(h, 200, gson.toJson(manager.getSubtaskById(incoming.getId())));
                    }
                } catch (IllegalArgumentException | IllegalStateException overlap) {
                    conflict406(h, overlap.getMessage());
                }
                return;
            }

            if ("DELETE".equals(method)) {
                Integer id = parseId(path, "/subtasks");
                if (id == null) {
                    sendText(h, 400, "id required");
                    return;
                }
                if (manager.getSubtaskById(id) == null) {
                    notFound(h);
                    return;
                }
                manager.deleteSubtaskById(id);
                sendText(h, 200, "deleted");
                return;
            }

            sendText(h, 405, "Method Not Allowed");
        } catch (Throwable ex) {
            serverError(h, ex);
        }
    }

    private void handleEpics(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            String path = h.getRequestURI().getPath();

            if ("GET".equals(method)) {
                if (path.matches("^/epics/\\d+/subtasks/?$")) {
                    Integer id = parseId(path, "/epics");
                    if (id == null) {
                        sendText(h, 400, "id required");
                        return;
                    }
                    if (manager.getEpicById(id) == null) {
                        notFound(h);
                        return;
                    }
                    sendJson(h, 200, gson.toJson(manager.getSubtasksByEpic(id)));
                    return;
                }

                Integer id = parseId(path, "/epics");
                if (id == null) {
                    sendJson(h, 200, gson.toJson(manager.getAllEpics()));
                } else {
                    Epic e = manager.getEpicById(id);
                    if (e == null) {
                        notFound(h);
                        return;
                    }
                    sendJson(h, 200, gson.toJson(e));
                }
                return;
            }

            if ("POST".equals(method)) {
                String body = readBody(h);
                Epic incoming = JsonUtil.parseEpic(body);
                if (incoming == null) {
                    sendText(h, 400, "Empty body");
                    return;
                }

                if (incoming.getId() <= 0) {
                    int id = manager.createEpic(incoming);
                    sendJson(h, 201, gson.toJson(manager.getEpicById(id)));
                } else {
                    if (manager.getEpicById(incoming.getId()) == null) {
                        notFound(h);
                        return;
                    }
                    manager.updateEpic(incoming);
                    sendJson(h, 200, gson.toJson(manager.getEpicById(incoming.getId())));
                }
                return;
            }

            if ("DELETE".equals(method)) {
                Integer id = parseId(path, "/epics");
                if (id == null) {
                    sendText(h, 400, "id required");
                    return;
                }
                if (manager.getEpicById(id) == null) {
                    notFound(h);
                    return;
                }
                manager.deleteEpicById(id);
                sendText(h, 200, "deleted");
                return;
            }

            sendText(h, 405, "Method Not Allowed");
        } catch (Throwable ex) {
            serverError(h, ex);
        }
    }

    private void handleHistory(HttpExchange h) throws IOException {
        try {
            if (!"GET".equals(h.getRequestMethod())) {
                sendText(h, 405, "Method Not Allowed");
                return;
            }
            sendJson(h, 200, gson.toJson(manager.getHistory()));
        } catch (Throwable ex) {
            serverError(h, ex);
        }
    }

    private void handlePrioritized(HttpExchange h) throws IOException {
        try {
            if (!"GET".equals(h.getRequestMethod())) {
                sendText(h, 405, "Method Not Allowed");
                return;
            }
            sendJson(h, 200, gson.toJson(manager.getPrioritizedTasks()));
        } catch (Throwable ex) {
            serverError(h, ex);
        }
    }
}

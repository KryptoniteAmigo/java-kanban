package manager.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import ru.practicum.yandex.tracker.http.HttpTaskServer;
import ru.practicum.yandex.tracker.manager.InMemoryTaskManager;
import ru.practicum.yandex.tracker.manager.TaskManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class HttpApiTestBase {

    protected TaskManager manager;
    protected HttpTaskServer server;
    protected HttpClient client;

    protected static final String BASE = "http://localhost:8080";

    @BeforeEach
    void setUp() throws Exception {
        manager = new InMemoryTaskManager();
        server = new HttpTaskServer(manager);
        client = HttpClient.newHttpClient();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    protected HttpResponse<String> GET(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> DELETE(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path)).DELETE().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> POST(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    protected static String jsonTask(String title, String desc,
                                     String startIso,
                                     Integer durationMinutes,
                                     Integer id) {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) {
            sb.append("\"id\":").append(id).append(',');
        }
        sb.append("\"title\":\"").append(title).append("\",")
                .append("\"description\":\"").append(desc).append("\"");
        if (startIso != null) {
            sb.append(",\"startTime\":\"").append(startIso).append("\"");
        }
        if (durationMinutes != null) {
            sb.append(",\"duration\":").append(durationMinutes);
        }
        sb.append('}');
        return sb.toString();
    }

    protected static String jsonEpic(String title, String desc, Integer id) {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) {
            sb.append("\"id\":").append(id).append(',');
        }
        sb.append("\"title\":\"").append(title).append("\",")
                .append("\"description\":\"").append(desc).append("\"");
        sb.append('}');
        return sb.toString();
    }

    protected static String jsonSubtask(String title, String desc, int epicId,
                                        String startIso, Integer durationMinutes, Integer id) {
        StringBuilder sb = new StringBuilder("{");
        if (id != null) {
            sb.append("\"id\":").append(id).append(',');
        }
        sb.append("\"title\":\"").append(title).append("\",")
                .append("\"description\":\"").append(desc).append("\",")
                .append("\"epicId\":").append(epicId);
        if (startIso != null) {
            sb.append(",\"startTime\":\"").append(startIso).append("\"");
        }
        if (durationMinutes != null) {
            sb.append(",\"duration\":").append(durationMinutes);
        }
        sb.append('}');
        return sb.toString();
    }
}

package manager.http;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class HttpCommonApiTest extends HttpApiTestBase {

    @Test
    public void history_returns200_andGrows_whenGetCalled() throws Exception {
        POST("/tasks", jsonTask("T1","", null, null, null));
        int id = manager.getAllTasks().get(0).getId();

        manager.getTaskById(id);
        var r = GET("/history");
        assertEquals(200, r.statusCode());
        assertFalse(manager.getHistory().isEmpty());
    }

    @Test
    public void prioritized_returns200_onlyTimed() throws Exception {
        POST("/tasks", jsonTask("Timed","", "2025-01-01T09:00", 60, null));
        POST("/tasks", jsonTask("NoTime","", null, null, null));

        var r = GET("/prioritized");
        assertEquals(200, r.statusCode());

        List<Task> pr = manager.getPrioritizedTasks();
        assertEquals(1, pr.size(), "В приоритете должны быть только задачи с временем");
        assertEquals("Timed", pr.get(0).getTitle());
    }
}

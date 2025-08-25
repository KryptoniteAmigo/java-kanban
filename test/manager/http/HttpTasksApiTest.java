package manager.http;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpTasksApiTest extends HttpApiTestBase {

    @Test
    public void createTask_returns201_andStored() throws Exception {
        var r = POST("/tasks", jsonTask("T1", "D", "2025-01-01T09:00", 45, null));
        assertEquals(201, r.statusCode());

        List<Task> all = manager.getAllTasks();
        assertEquals(1, all.size());
        assertEquals("T1", all.get(0).getTitle());
        assertEquals(Duration.ofMinutes(45), all.get(0).getDuration());
        assertEquals(LocalDateTime.of(2025,1,1,9,0), all.get(0).getStartTime());
    }

    @Test
    public void getTaskById_returns200_or404() throws Exception {
        POST("/tasks", jsonTask("T1", "D", null, null, null));
        int id = manager.getAllTasks().get(0).getId();

        assertEquals(200, GET("/tasks/" + id).statusCode());
        assertEquals(404, GET("/tasks/999999").statusCode());
    }

    @Test
    public void updateTask_returns200_andAppliesChanges() throws Exception {
        POST("/tasks", jsonTask("Old", "D", null, null, null));
        var t = manager.getAllTasks().get(0);

        var r = POST("/tasks", jsonTask("New", "ND", "2025-01-01T08:00", 30, t.getId()));
        assertEquals(200, r.statusCode());

        var after = manager.getTaskById(t.getId());
        assertEquals("New", after.getTitle());
        assertEquals("ND", after.getDescription());
        assertEquals(Duration.ofMinutes(30), after.getDuration());
        assertEquals(LocalDateTime.of(2025,1,1,8,0), after.getStartTime());
    }

    @Test
    public void deleteTaskById_returns200_andRemoves() throws Exception {
        POST("/tasks", jsonTask("T", "D", null, null, null));
        int id = manager.getAllTasks().get(0).getId();

        assertEquals(200, DELETE("/tasks/" + id).statusCode());
        assertTrue(manager.getAllTasks().isEmpty());
        assertEquals(404, DELETE("/tasks/" + id).statusCode());
    }

    @Test
    public void createTask_overlap_returns406() throws Exception {
        POST("/tasks", jsonTask("A", "D","2025-01-01T10:00", 60, null));
        var r = POST("/tasks", jsonTask("B", "D","2025-01-01T10:30", 30, null));
        assertEquals(406, r.statusCode(), "Должен вернуться 406 при пересечении");
    }
}

package manager.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpSubtasksApiTest extends HttpApiTestBase {

    @Test
    public void createSubtask_requiresEpic_returns201() throws Exception {
        POST("/epics", jsonEpic("E", "D", null));
        int epicId = manager.getAllEpics().get(0).getId();

        var r = POST("/subtasks", jsonSubtask("S", "D", epicId, "2025-01-01T10:00", 30, null));
        assertEquals(201, r.statusCode());

        var s = manager.getAllSubtasks().get(0);
        assertEquals("S", s.getTitle());
        assertEquals(epicId, s.getEpicId());
        assertEquals(Duration.ofMinutes(30), s.getDuration());
        assertEquals(LocalDateTime.of(2025,1,1,10,0), s.getStartTime());
    }

    @Test
    public void createSubtask_forMissingEpic_returns404() throws Exception {
        var r = POST("/subtasks", jsonSubtask("S", "D", 999999, null, null, null));
        assertEquals(404, r.statusCode());
    }

    @Test
    public void createSubtask_overlap_returns406() throws Exception {
        POST("/epics", jsonEpic("E", "D", null));
        int epicId = manager.getAllEpics().get(0).getId();

        POST("/subtasks", jsonSubtask("S1","", epicId, "2025-01-01T10:00", 60, null));
        var r = POST("/subtasks", jsonSubtask("S2","", epicId, "2025-01-01T10:30", 30, null));
        assertEquals(406, r.statusCode());
    }

    @Test
    public void updateSubtask_changes_andRecalcEpic() throws Exception {
        POST("/epics", jsonEpic("E", "D", null));
        int epicId = manager.getAllEpics().get(0).getId();

        POST("/subtasks", jsonSubtask("S","D", epicId, "2025-01-01T10:00", 30, null));
        var sub = manager.getAllSubtasks().get(0);

        var r = POST("/subtasks", jsonSubtask("S2","ND", epicId, "2025-01-01T11:00", 20, sub.getId()));
        assertEquals(200, r.statusCode());

        var after = manager.getSubtaskById(sub.getId());
        assertEquals("S2", after.getTitle());
        assertEquals("ND", after.getDescription());
        assertEquals(LocalDateTime.of(2025,1,1,11,0), after.getStartTime());
        assertEquals(Duration.ofMinutes(20), after.getDuration());
    }
}

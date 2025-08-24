package manager.http;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.model.Epic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpEpicsApiTest extends HttpApiTestBase {

    @Test
    public void createEpic_returns201() throws Exception {
        var r = POST("/epics", jsonEpic("E", "D", null));
        assertEquals(201, r.statusCode());

        List<Epic> all = manager.getAllEpics();
        assertEquals(1, all.size());
        assertEquals("E", all.get(0).getTitle());
    }

    @Test
    public void deleteEpic_removesSubtasksToo() throws Exception {
        POST("/epics", jsonEpic("E", "D", null));
        int epicId = manager.getAllEpics().get(0).getId();

        POST("/subtasks", jsonSubtask("S1","", epicId, null, null, null));
        POST("/subtasks", jsonSubtask("S2","", epicId, null, null, null));

        assertFalse(manager.getAllSubtasks().isEmpty());

        assertEquals(200, DELETE("/epics/" + epicId).statusCode());
        assertTrue(manager.getAllEpics().isEmpty());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }
}

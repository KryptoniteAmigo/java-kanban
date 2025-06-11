package manager;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManagersTest {

    @Test
    void getDefaultManagersNotNull() {
        TaskManager tm = Managers.getDefault();
        HistoryManager hm = Managers.getDefaultHistory();
        assertNotNull(tm, "Managers.getDefault() не должен возвращать null");
        assertNotNull(hm, "Managers.getDefaultHistory() не должен возвращать null");
    }

    @Test
    void getDefaultManagersHaveCorrectType() {
        TaskManager tm = Managers.getDefault();
        HistoryManager hm = Managers.getDefaultHistory();
        assertTrue(tm instanceof InMemoryTaskManager,
                "Managers.getDefault() должен возвращать InMemoryTaskManager");
        assertTrue(hm instanceof InMemoryHistoryManager,
                "Managers.getDefaultHistory() должен возвращать InMemoryHistoryManager");
    }
}

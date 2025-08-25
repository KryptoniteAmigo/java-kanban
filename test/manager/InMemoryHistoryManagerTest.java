package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.HistoryManager;
import ru.practicum.yandex.tracker.manager.Managers;
import ru.practicum.yandex.tracker.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemoryHistoryManagerTest {
    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = Managers.getDefaultHistory();
    }

    @Test
    public void shouldBeUnboundedAndKeepOrder() {
        HistoryManager hm = Managers.getDefaultHistory();
        for (int i = 1; i <= 20; i++) {
            hm.add(new Task(i, "T"+i, "D"));
        }
        List<Task> history = hm.getHistory();
        assertEquals(20, history.size());
        for (int i = 0; i < 20; i++) {
            assertEquals(i + 1, history.get(i).getId());
        }
    }


    @Test
    public void shouldDeduplicateAndKeepLastOccurrence() {
        HistoryManager hm = Managers.getDefaultHistory();
        Task t1 = new Task(1, "A", "");
        Task t2 = new Task(2, "B", "");
        hm.add(t1);
        hm.add(t2);
        hm.add(t1);

        List<Task> history = hm.getHistory();
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getId());
        assertEquals(1, history.get(1).getId());
    }

    @Test
    public void shouldNotContainDuplicates() {
        Task t = new Task(42, "Answer", "Desc");
        historyManager.add(t);
        historyManager.add(t);                             // добавляем повторно
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(42, history.get(0).getId());
    }
}

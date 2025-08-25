package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.HistoryManager;
import ru.practicum.yandex.tracker.manager.Managers;
import ru.practicum.yandex.tracker.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        historyManager.add(t);
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(42, history.get(0).getId());
    }

    @Test
    public void emptyHistory_returnsEmptyList() {
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    public void addDoesNotTrimAndKeepsOrder() {
        for (int i = 1; i <= 12; i++) {
            Task t = new Task(i, "T" + i, "");
            historyManager.add(t);
        }

        List<Task> h = historyManager.getHistory();
        assertEquals(12, h.size(), "История не должна обрезаться до 10");
        assertEquals(1, h.get(0).getId());
        assertEquals(12, h.get(11).getId());
    }

    @Test
    public void remove_head_middle_tail() {
        HistoryManager hm = Managers.getDefaultHistory();

        Task t1 = new Task(0,"a",""); t1.setId(1);
        Task t2 = new Task(0,"b",""); t2.setId(2);
        Task t3 = new Task(0,"c",""); t3.setId(3);

        hm.add(t1);
        hm.add(t2);
        hm.add(t3);

        hm.remove(1);
        assertEquals(2, hm.getHistory().size());
        assertEquals(2, hm.getHistory().get(0).getId());

        hm.remove(2);
        assertEquals(1, hm.getHistory().size());
        assertEquals(3, hm.getHistory().get(0).getId());

        hm.remove(3);
        assertTrue(hm.getHistory().isEmpty());
    }
}

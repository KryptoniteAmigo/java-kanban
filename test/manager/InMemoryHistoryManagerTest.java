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
    public void shouldKeepOnlyLast10() {
        for (int i = 1; i <= 11; i++) {                     // добавляем 11 задач
            historyManager.add(new Task(i, "Task " + i, "Desc"));
        }
        List<Task> history = historyManager.getHistory();
        assertEquals(10, history.size());          // проверяем размер и что самой первой осталась Task с id=2
        assertEquals(2, history.get(0).getId());
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

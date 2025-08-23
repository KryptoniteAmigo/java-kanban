package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.TaskManager;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Status;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTestBase<T extends TaskManager> {
    protected T manager;

    protected abstract T createManager() throws Exception;

    @BeforeEach
    void setUp() throws Exception {
        manager = createManager();
    }

    protected Epic createEpic(String title) {
        Epic e = new Epic(0, title, "");
        int id = manager.createEpic(e);
        return manager.getEpicById(id);
    }

    protected Subtask createSub(Epic e, String title, Status st,
                                LocalDateTime start, Duration dur) {
        Subtask s = new Subtask(0, title, "", e.getId());
        s.setStatus(st);
        s.setStartTime(start);
        s.setDuration(dur);
        int sid = manager.createSubtask(s);
        return manager.getSubtaskById(sid);
    }

    protected Subtask createSub(Epic e, String title, Status st) {
        return createSub(e, title, st, null, null);
    }

    protected Task createTask(String title, LocalDateTime start, Duration dur) {
        Task t = new Task(0, title, "");
        t.setStartTime(start);
        t.setDuration(dur);
        int id = manager.createTask(t);
        return manager.getTaskById(id);
    }

    protected Task createTask(String title) {
        return createTask(title, null, null);
    }

    @Test
    public void epicStatus_allSubtasksNEW_shouldBeNEW() {
        Epic e = createEpic("E");
        createSub(e, "s1", Status.NEW, null, null);
        createSub(e, "s2", Status.NEW, null, null);

        assertEquals(Status.NEW, manager.getEpicById(e.getId()).getStatus());
    }

    @Test
    public void epicStatus_allSubtasksDONE_shouldBeDONE() {
        Epic e = createEpic("E");
        createSub(e, "s1", Status.DONE, null, null);
        createSub(e, "s2", Status.DONE, null, null);

        assertEquals(Status.DONE, manager.getEpicById(e.getId()).getStatus());
    }

    @Test
    public void epicStatus_mixedNEWandDONE_shouldBeIN_PROGRESS() {
        Epic e = createEpic("E");
        createSub(e, "s1", Status.NEW, null, null);
        createSub(e, "s2", Status.DONE, null, null);

        assertEquals(Status.IN_PROGRESS, manager.getEpicById(e.getId()).getStatus());
    }

    @Test
    public void epicStatus_singleSubtaskIN_PROGRESS_shouldBeIN_PROGRESS() {
        Epic e = createEpic("E");
        createSub(e, "s", Status.IN_PROGRESS, null, null);

        assertEquals(Status.IN_PROGRESS, manager.getEpicById(e.getId()).getStatus());
    }

    @Test
    public void getPrioritizedTasks_shouldReturnSortedByStartTime_nullsLast() {
        Task t2 = createTask("B",
                LocalDateTime.of(2025, 1, 1, 8, 0), Duration.ofMinutes(30));
        Task t1 = createTask("A",
                LocalDateTime.of(2025, 1, 1, 9, 0), Duration.ofMinutes(60));
        Task t3 = createTask("C", null, null);

        List<Task> prio = manager.getPrioritizedTasks();
        assertEquals(2, prio.size(), "В списке приоритетов должны быть только задачи с временем");
        assertEquals(t2.getId(), prio.get(0).getId());
        assertEquals(t1.getId(), prio.get(1).getId());
    }

    @Test
    public void creatingOverlappingTasks_shouldThrow() {
        createTask("A", LocalDateTime.of(2025, 1, 1, 10, 0), Duration.ofMinutes(60));

        Task overlapped = new Task(0,"B", "");
        overlapped.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 30));
        overlapped.setDuration(Duration.ofMinutes(30));

        assertThrows(IllegalArgumentException.class, () -> manager.createTask(overlapped));
    }

    @Test
    public void updatingToOverlappingInterval_shouldThrow_andKeepOldState() {
        Task t1 = createTask("A", LocalDateTime.of(2025, 1, 1, 10, 0), Duration.ofMinutes(60));
        Task t2 = createTask("B", LocalDateTime.of(2025, 1, 1, 12, 0), Duration.ofMinutes(30));

        t2.setStartTime(LocalDateTime.of(2025, 1, 1, 10, 30));
        assertThrows(IllegalArgumentException.class, () -> manager.updateTask(t2));

        Task after = manager.getTaskById(t2.getId());
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0), after.getStartTime(),
                "При неудачном обновлении старые значения не должны меняться");
    }

    @Test
    public void subtaskMustHaveExistingEpic() {
        Subtask s = new Subtask(0,"S", "", 999_999);
        assertThrows(IllegalArgumentException.class, () -> manager.createSubtask(s));
    }

    @Test
    public void deletingTask_shouldRemoveItFromHistory() {
        Task t = createTask("T", null, null);
        manager.getTaskById(t.getId());
        manager.deleteTaskById(t.getId());

        boolean present = manager.getHistory().stream().anyMatch(x -> x.getId() == t.getId());
        assertFalse(present, "Удалённая задача не должна оставаться в истории");
    }

    @Test
    public void deletingEpic_shouldRemoveEpicAndSubtasksFromHistory() {
        Epic e = createEpic("E");
        Subtask s1 = createSub(e, "s1", Status.NEW, null, null);
        Subtask s2 = createSub(e, "s2", Status.NEW, null, null);

        manager.getEpicById(e.getId());
        manager.getSubtaskById(s1.getId());
        manager.getSubtaskById(s2.getId());
        manager.deleteEpicById(e.getId());

        boolean present = manager.getHistory().stream()
                .anyMatch(x -> x.getId() == e.getId()
                        || x.getId() == s1.getId()
                        || x.getId() == s2.getId());
        assertFalse(present, "После удаления эпика его сабтаски и сам эпик должны исчезнуть из истории");
    }
}

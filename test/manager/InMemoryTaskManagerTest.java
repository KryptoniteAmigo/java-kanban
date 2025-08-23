package manager;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.InMemoryTaskManager;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Status;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest extends TaskManagerTestBase<InMemoryTaskManager> {

    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }

    @Test
    public void epicWithoutSubtasks_hasZeroDurationAndNullTimes() {
        Epic epic = new Epic(0, "E", "D");
        int epicId = manager.createEpic(epic);

        Epic reloaded = manager.getEpicById(epicId);
        assertNotNull(reloaded);
        assertEquals(Duration.ZERO, reloaded.getDuration(), "У пустого эпика duration должен быть 0");
        assertNull(reloaded.getStartTime(), "У пустого эпика startTime должен быть null");
        assertNull(reloaded.getEndTime(),   "У пустого эпика endTime должен быть null");
    }

    @Test
    public void epicWithAllNewSubtasks_statusNEW_andTimeAggregated() {
        Epic epic = new Epic(0, "E", "D");
        int epicId = manager.createEpic(epic);

        Subtask s1 = new Subtask(0, "S1", "D1", epicId);
        s1.setStatus(Status.NEW);
        s1.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        s1.setDuration(Duration.ofMinutes(60));
        manager.createSubtask(s1);

        Subtask s2 = new Subtask(0, "S2", "D2", epicId);
        s2.setStatus(Status.NEW);
        s2.setStartTime(LocalDateTime.of(2025, 1, 1, 11, 0));
        s2.setDuration(Duration.ofMinutes(30));
        manager.createSubtask(s2);

        Epic re = manager.getEpicById(epicId);
        assertEquals(Status.NEW, re.getStatus(), "Если все подзадачи NEW — эпик NEW");
        assertEquals(Duration.ofMinutes(90), re.getDuration(), "Сумма duration сабтасков");
        assertEquals(LocalDateTime.of(2025,1,1,9,0),  re.getStartTime(), "min start");
        assertEquals(LocalDateTime.of(2025,1,1,11,30), re.getEndTime(),   "max end");
    }

    @Test
    public void prioritizedReturnsTasksAndSubtasksSortedByStartTime_onlyWithTimes() {
        Task t1 = new Task(0, "T1", "D1");
        t1.setStartTime(LocalDateTime.of(2025,1,1,9,0));
        t1.setDuration(Duration.ofMinutes(60));
        int id1 = manager.createTask(t1);

        Task tNoTime = new Task(0, "T-NT", "D");
        int idNoTime = manager.createTask(tNoTime);

        Epic epic = new Epic(0, "E", "D");
        int epicId = manager.createEpic(epic);

        Subtask s1 = new Subtask(0, "S1", "D", epicId);
        s1.setStartTime(LocalDateTime.of(2025,1,1,10,15));
        s1.setDuration(Duration.ofMinutes(30));
        int sid1 = manager.createSubtask(s1);

        List<Task> pr = manager.getPrioritizedTasks();
        assertEquals(2, pr.size(), "В списке приоритетов — только задачи с временем");

        assertEquals(id1,  pr.get(0).getId());
        assertEquals(sid1, pr.get(1).getId());
        assertTrue(pr.stream().noneMatch(t -> t.getId() == idNoTime));
    }

    @Test
    public void deleteTaskRemovesFromPrioritized() {
        Task t = new Task(0, "T", "D");
        t.setStartTime(LocalDateTime.of(2025,1,1,9,0));
        t.setDuration(Duration.ofMinutes(30));
        int id = manager.createTask(t);

        assertFalse(manager.getPrioritizedTasks().isEmpty());
        manager.deleteTaskById(id);
        assertTrue(manager.getPrioritizedTasks().isEmpty(), "После удаления задача должна пропасть из приоритета");
    }
}

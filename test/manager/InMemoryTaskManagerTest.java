package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.Managers;
import ru.practicum.yandex.tracker.manager.TaskManager;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Status;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {
    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = Managers.getDefault();
    }

    //Task
    @Test
    public void shouldCreateAndRetrieveTask() {
        Task t = new Task(0, "New", "Desc");
        int id = manager.createTask(t);
        Task fetched = manager.getTaskById(id);
        assertNotNull(fetched);
        assertEquals("New", fetched.getTitle());
    }

    @Test
    public void shouldDeleteAllTasks() {
        manager.createTask(new Task(0, "A", ""));
        manager.createTask(new Task(0, "B", ""));
        manager.deleteAllTasks();
        assertTrue(manager.getAllTasks().isEmpty());
    }

    @Test
    public void shouldUpdateTask() {
        Task t = new Task(0, "Old title", "Old desc");
        int id = manager.createTask(t);

        t.setTitle("New title");
        t.setDescription("New desc");
        t.setStatus(Status.IN_PROGRESS);
        manager.updateTask(t);

        Task updated = manager.getTaskById(id);
        assertNotNull(updated, "Обновлённая задача должна быть найдена");
        assertEquals("New title", updated.getTitle(), "Заголовок должен обновиться");
        assertEquals("New desc", updated.getDescription(), "Описание должно обновиться");
        assertEquals(Status.IN_PROGRESS, updated.getStatus(), "Статус должен обновиться");
    }

    @Test
    public void shouldDeleteTaskById() {
        Task t1 = new Task(0, "A", "");
        Task t2 = new Task(0, "B", "");
        int id1 = manager.createTask(t1);
        int id2 = manager.createTask(t2);

        manager.deleteTaskById(id1);
        assertNull(manager.getTaskById(id1), "После удаления getTaskById должен вернуть null");
        List<Task> all = manager.getAllTasks();
        assertEquals(1, all.size(), "Должна остаться ровно одна задача");
        assertEquals(id2, all.get(0).getId(), "Должен остаться именно второй таск");
    }

    @Test
    public void shouldGetAllTasks() {
        assertTrue(manager.getAllTasks().isEmpty(), "Список задач изначально пуст");

        Task t1 = new Task(0, "X", "");
        Task t2 = new Task(0, "Y", "");
        int id1 = manager.createTask(t1);
        int id2 = manager.createTask(t2);

        List<Task> all = manager.getAllTasks();
        assertEquals(2, all.size(), "После добавления двух задач размер списка должен быть 2");
        assertTrue(all.stream().anyMatch(t -> t.getId() == id1), "Список должен содержать первую задачу");
        assertTrue(all.stream().anyMatch(t -> t.getId() == id2), "Список должен содержать вторую задачу");
    }

    @Test
    public void shouldRecordHistoryOnGet() {
        int id = manager.createTask(new Task(0, "X", ""));
        manager.getTaskById(id);
        manager.getTaskById(id);
        List<Task> hist = manager.getHistory();
        assertEquals(1, hist.size());
        assertEquals(id, hist.get(0).getId());
    }

    @Test
    public void shouldKeepTaskFieldsUnchangedAfterAdd() {
        Task original = new Task(0, "OrigTitle", "OrigDesc");
        original.setStatus(Status.NEW);

        String beforeTitle = original.getTitle();
        String beforeDesc = original.getDescription();
        Status beforeStatus = original.getStatus();

        int assignedId = manager.createTask(original);

        assertEquals(beforeTitle, original.getTitle(), "Title не должен меняться");
        assertEquals(beforeDesc, original.getDescription(), "Description не должен меняться");
        assertEquals(beforeStatus, original.getStatus(), "Status не должен меняться");

        Task fetched = manager.getTaskById(assignedId);
        assertNotNull(fetched, "По возвращённому id должна вернуться задача");
    }

    @Test
    public void shouldKeepSnapshotInHistoryAfterTaskModification() {
        Task task = new Task(0, "OrigTitle", "OrigDesc");
        int id = manager.createTask(task);
        manager.getTaskById(id);

        task.setTitle("ModifiedTitle");
        task.setDescription("ModifiedDesc");
        task.setStatus(Status.IN_PROGRESS);

        List<Task> history = manager.getHistory();
        assertEquals(1, history.size(), "В истории должен быть ровно один элемент");
        Task hist = history.get(0);

        assertEquals("OrigTitle", hist.getTitle(), "Заголовок должен быть оригинальным");
        assertEquals("OrigDesc", hist.getDescription(),"Описание должно быть оригинальным");
        assertEquals(Status.NEW, hist.getStatus(), "Статус должен быть исходным (NEW)");
    }

    //SubTask
    @Test
    public void shouldCreateAndRetrieveSubtask() {
        Epic epic = new Epic(0, "Parent Epic", "");
        int epicId = manager.createEpic(epic);

        Subtask sub = new Subtask(0, "Subtask 1", "Desc", epicId);
        int subId = manager.createSubtask(sub);
        Subtask fetched = manager.getSubtaskById(subId);
        assertNotNull(fetched);
        assertEquals("Subtask 1", fetched.getTitle());
        assertEquals(epicId, fetched.getEpicId());

        List<Subtask> allSubs = manager.getAllSubtasks();
        assertTrue(allSubs.contains(fetched));
    }

    @Test
    public void shouldUpdateSubtask() {
        Epic epic = new Epic(0, "X", "");
        int epicId = manager.createEpic(epic);

        Subtask sub = new Subtask(0, "Old", "OldDesc", epicId);
        int subId = manager.createSubtask(sub);

        sub.setTitle("Updated");
        sub.setDescription("UpdatedDesc");
        manager.updateSubtask(sub);

        Subtask updated = manager.getSubtaskById(subId);
        assertEquals("Updated", updated.getTitle());
        assertEquals("UpdatedDesc", updated.getDescription());
    }

    @Test
    public void shouldDeleteSubtaskById() {
        Epic epic = new Epic(0, "E", "");
        int epicId = manager.createEpic(epic);

        Subtask sub = new Subtask(0, "S", "", epicId);
        int subId = manager.createSubtask(sub);

        manager.deleteSubtaskById(subId);
        assertNull(manager.getSubtaskById(subId));
        assertFalse(manager.getAllSubtasks().stream().anyMatch(s -> s.getId() == subId));
    }

    @Test
    public void shouldDeleteAllSubtasks() {
        Epic epic = new Epic(0, "E", "");
        int epicId = manager.createEpic(epic);

        manager.createSubtask(new Subtask(0, "S1", "", epicId));
        manager.createSubtask(new Subtask(0, "S2", "", epicId));
        manager.deleteAllSubtasks();

        assertTrue(manager.getAllSubtasks().isEmpty(), "Все сабтаски должны удалиться");
        assertTrue(manager.getSubtasksByEpic(epicId).isEmpty(), "Список сабтасков у эпика также пуст");
    }

    //Epic
    @Test
    public void shouldCreateAndRetrieveEpic() {
        Epic epic = new Epic(0, "Epic Title", "Epic Description");
        int epicId = manager.createEpic(epic);
        Epic fetched = manager.getEpicById(epicId);
        assertNotNull(fetched, "Эпик должен вернуться по id");
        assertEquals("Epic Title", fetched.getTitle());
        assertEquals("Epic Description", fetched.getDescription());

        List<Epic> allEpics = manager.getAllEpics();
        assertTrue(allEpics.contains(fetched), "Список всех эпиков должен содержать созданный");
    }

    @Test
    public void shouldUpdateEpic() {
        Epic epic = new Epic(0, "Old Title", "Old Desc");
        int epicId = manager.createEpic(epic);

        epic.setTitle("New Title");
        epic.setDescription("New Desc");
        manager.updateEpic(epic);

        Epic updated = manager.getEpicById(epicId);
        assertEquals("New Title", updated.getTitle());
        assertEquals("New Desc", updated.getDescription());
    }

    @Test
    public void shouldDeleteEpicById() {
        Epic epic = new Epic(0, "E", "D");
        int epicId = manager.createEpic(epic);

        manager.deleteEpicById(epicId);
        assertNull(manager.getEpicById(epicId), "После удаления по id эпик должен быть null");
        assertTrue(manager.getAllEpics().isEmpty(), "Список эпиков после удаления должен быть пуст");
    }

    @Test
    public void shouldDeleteAllEpics() {
        manager.createEpic(new Epic(0, "E1", ""));
        manager.createEpic(new Epic(0, "E2", ""));
        manager.deleteAllEpics();
        assertTrue(manager.getAllEpics().isEmpty(), "После deleteAllEpics список эпиков пуст");
    }

    @Test
    public void shouldRecordHistoryOnGetSubtaskAndEpic() {
        Epic epic = new Epic(0, "EPIC", "EDESC");
        int epicId = manager.createEpic(epic);

        Subtask sub = new Subtask(0, "SUB", "SDESC", epicId);
        int subId = manager.createSubtask(sub);

        Epic fetchedEpic = manager.getEpicById(epicId);
        Subtask fetchedSub = manager.getSubtaskById(subId);

        List<Task> history = manager.getHistory();
        assertEquals(2, history.size(), "В истории должно быть два просмотра");
        assertEquals(epicId, history.get(0).getId(), "Первым должен быть эпик");
        assertEquals(subId,   history.get(1).getId(), "Вторым — сабтаск");
    }

}

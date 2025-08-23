package manager;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.FileBackedTaskManager;
import ru.practicum.yandex.tracker.manager.ManagerSaveException;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends TaskManagerTestBase<FileBackedTaskManager> {
    private File tempFile() throws IOException {
        File f = File.createTempFile("kanban-", ".csv");
        f.deleteOnExit();
        return f;
    }

    @Override
    protected FileBackedTaskManager createManager() throws Exception {
        return new FileBackedTaskManager(tempFile());
    }

    @Test
    public void saveAndLoadEmptyFile() throws IOException {
        File file = tempFile();
        FileBackedTaskManager m = new FileBackedTaskManager(file);
        int id = m.createTask(new Task(0, "tmp", ""));
        m.deleteTaskById(id);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);
        assertTrue(loaded.getAllTasks().isEmpty());
        assertTrue(loaded.getAllEpics().isEmpty());
        assertTrue(loaded.getAllSubtasks().isEmpty());
    }

    @Test
    public void saveSeveralAndLoadBack() throws IOException {
        File file = tempFile();
        FileBackedTaskManager m = new FileBackedTaskManager(file);

        int t1 = m.createTask(new Task(0, "T1", "D1"));
        int t2 = m.createTask(new Task(0, "T2", "D2"));
        int e1 = m.createEpic(new Epic(0, "E1", "ED"));
        int s1 = m.createSubtask(new Subtask(0, "S1", "SD1", e1));
        int s2 = m.createSubtask(new Subtask(0, "S2", "SD2", e1));

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);
        List<Task> tasks = loaded.getAllTasks();
        List<Epic> epics = loaded.getAllEpics();
        List<Subtask> subs = loaded.getAllSubtasks();

        assertEquals(2, tasks.size(), "Должно загрузиться 2 задачи");
        assertEquals(1, epics.size(), "Должен загрузиться 1 эпик");
        assertEquals(2, subs.size(), "Должно загрузиться 2 сабтаска");

        Epic epic = epics.get(0);
        for (int i = 0; i < subs.size(); i++) {
            assertEquals(epic.getId(), subs.get(i).getEpicId(), "Неверная связь сабтаска с эпиком");
        }
        List<Subtask> subsOfEpic = loaded.getSubtasksByEpic(epic.getId());
        assertEquals(2, subsOfEpic.size());
    }

    @Test
    public void loadFromExistingFileWithIdsShouldContinueGeneratingUniqueIds() throws IOException {
        File file = tempFile();
        FileBackedTaskManager m = new FileBackedTaskManager(file);

        int a = m.createTask(new Task(0, "A", ""));
        int b = m.createTask(new Task(0, "B", ""));

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);
        int c = loaded.createTask(new Task(0, "C", ""));
        assertTrue(c > a && c > b, "Сгенерированный id должен быть больше существующих");
    }

    @Test
    public void saveAndLoadData_keepsFieldsAndEpicTime() throws IOException {
        File file = tempFile();
        FileBackedTaskManager m = new FileBackedTaskManager(file);

        Task t = new Task(0, "T", "D");
        t.setStartTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        t.setDuration(Duration.ofMinutes(45));
        int tId = m.createTask(t);

        Epic e = new Epic(0, "E", "D");
        int eId = m.createEpic(e);

        Subtask s1 = new Subtask(0, "S1", "D1", eId);
        s1.setStartTime(LocalDateTime.of(2025,1,1,10, 0));
        s1.setDuration(Duration.ofMinutes(30));
        int s1Id = m.createSubtask(s1);

        Subtask s2 = new Subtask(0, "S2", "D2", eId);
        s2.setStartTime(LocalDateTime.of(2025,1,1,11,30));
        s2.setDuration(Duration.ofMinutes(20));
        int s2Id = m.createSubtask(s2);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);

        assertEquals(1, loaded.getAllTasks().size());
        assertEquals(1, loaded.getAllEpics().size());
        assertEquals(2, loaded.getAllSubtasks().size());

        Task lt = loaded.getTaskById(tId);
        assertNotNull(lt);
        assertEquals(LocalDateTime.of(2025,1,1,9,0), lt.getStartTime());
        assertEquals(Duration.ofMinutes(45), lt.getDuration());

        Epic le = loaded.getEpicById(eId);
        assertNotNull(le);
        assertEquals(Duration.ofMinutes(50), le.getDuration());
        assertEquals(LocalDateTime.of(2025,1,1,10,0), le.getStartTime());
        assertEquals(LocalDateTime.of(2025,1,1,11,50), le.getEndTime());

        List<Subtask> subs = loaded.getSubtasksByEpic(eId);
        assertEquals(2, subs.size(), "Должно восстановиться 2 сабтаска для эпика");
    }

    @Test
    public void saveToInvalidPath_shouldThrowManagerSaveException() throws Exception {
        File dir = Files.createTempDirectory("kanban-dir").toFile();
        dir.deleteOnExit();

        FileBackedTaskManager m = new FileBackedTaskManager(dir);
        Task t = new Task(0,"A","");

        assertThrows(ManagerSaveException.class, () -> m.createTask(t));
    }

    @Test
    public void loadFromGarbage_shouldThrowManagerSaveException() throws Exception {
        File file = File.createTempFile("kanban", ".csv");
        file.deleteOnExit();
        Files.writeString(file.toPath(), "broken, csv, here");

        assertThrows(ManagerSaveException.class, () -> FileBackedTaskManager.loadFromFile(file));
    }
}

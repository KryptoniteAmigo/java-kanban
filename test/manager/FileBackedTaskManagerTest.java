package manager;

import org.junit.jupiter.api.Test;
import ru.practicum.yandex.tracker.manager.FileBackedTaskManager;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest {
    private File tempFile() throws IOException {
        File f = File.createTempFile("kanban-", ".csv");
        f.deleteOnExit();
        return f;
    }

    @Test
    void saveAndLoadEmptyFile() throws IOException {
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
    void saveSeveralAndLoadBack() throws IOException {
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
    void loadFromExistingFileWithIdsShouldContinueGeneratingUniqueIds() throws IOException {
        File file = tempFile();
        FileBackedTaskManager m = new FileBackedTaskManager(file);

        int a = m.createTask(new Task(0, "A", ""));
        int b = m.createTask(new Task(0, "B", ""));

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);
        int c = loaded.createTask(new Task(0, "C", ""));
        assertTrue(c > a && c > b, "Сгенерированный id должен быть больше существующих");
    }
}

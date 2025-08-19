package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    // Task
    @Override
    public int createTask(Task task) {
        int id = super.createTask(task);
        save();
        return id;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void deleteTaskById(int id) {
        super.deleteTaskById(id);
        save();
    }

    @Override
    public void deleteAllTasks() {
        super.deleteAllTasks();
        save();
    }

    // Subtask
    @Override
    public int createSubtask(Subtask subtask) {
        int id = super.createSubtask(subtask);
        save();
        return id;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteSubtaskById(int id) {
        super.deleteSubtaskById(id);
        save();
    }

    @Override
    public void deleteAllSubtasks() {
        super.deleteAllSubtasks();
        save();
    }

    // Epic
    @Override
    public int createEpic(Epic epic) {
        int id = super.createEpic(epic);
        save();
        return id;
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void deleteEpicById(int id) {
        super.deleteEpicById(id);
        save();
    }

    @Override
    public void deleteAllEpics() {
        super.deleteAllEpics();
        save();
    }

    private static final String HEADER = "id,type,name,status,description,epic";

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\n', ' ');
    }

    private static String taskToString(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append(task.getId()).append(',');
        if (task instanceof Epic) {
            sb.append(TaskType.EPIC);
        } else if (task instanceof Subtask) {
            sb.append(TaskType.SUBTASK);
        } else {
            sb.append(TaskType.TASK);
        }
        sb.append(',')
                .append(escape(task.getTitle())).append(',')
                .append(task.getStatus()).append(',')
                .append(escape(task.getDescription())).append(',');

        if (task instanceof Subtask) {
            sb.append(((Subtask) task).getEpicId());
        } else {
            sb.append("");
        }
        return sb.toString();
    }

    private static Task taskFromString(String value) {
        String[] f = value.split(",", -1);
        int id = Integer.parseInt(f[0]);
        TaskType type = TaskType.valueOf(f[1]);
        String name = f[2];
        Status status = Status.valueOf(f[3]);
        String desc = f[4];

        if (type == TaskType.TASK) {
            Task t = new Task(id, name, desc);
            t.setStatus(status);
            return t;
        } else if (type == TaskType.EPIC) {
            Epic e = new Epic(id, name, desc);
            e.setStatus(status);
            return e;
        } else {
            int epicId = Integer.parseInt(f[5]);
            Subtask s = new Subtask(id, name, desc, epicId);
            s.setStatus(status);
            return s;
        }
    }

    private void save() {
        try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            bw.write(HEADER);
            bw.newLine();
            List<Task> tasks = getAllTasks();
            for (int i = 0; i < tasks.size(); i++) {
                bw.write(taskToString(tasks.get(i)));
                bw.newLine();
            }
            List<Epic> epics = getAllEpics();
            for (int i = 0; i < epics.size(); i++) {
                bw.write(taskToString(epics.get(i)));
                bw.newLine();
            }
            List<Subtask> subs = getAllSubtasks();
            for (int i = 0; i < subs.size(); i++) {
                bw.write(taskToString(subs.get(i)));
                bw.newLine();
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Failed to save manager to file: " + file, e);
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager m = new FileBackedTaskManager(file);
        List<String> lines;

        try {
            if (!file.exists() || Files.size(file.toPath()) == 0L) {
                return m;
            }
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            lines = new ArrayList<String>();
            String[] arr = content.split("\\R");
            for (int i = 0; i < arr.length; i++) {
                lines.add(arr[i]);
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Failed to load manager from file: " + file, e);
        }

        int maxId = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            if (i == 0 && line.startsWith("id,")) {
                continue;
            }

            Task t = taskFromString(line);
            if (t instanceof Epic) {
                m.putEpicDirect((Epic) t);
            } else if (t instanceof Subtask) {
                m.putSubtaskDirect((Subtask) t);
            } else {
                m.putTaskDirect(t);
            }
            if (t.getId() > maxId) {
                maxId = t.getId();
            }
        }
        m.setNextIdAfterLoad(maxId + 1);
        return m;
    }
}

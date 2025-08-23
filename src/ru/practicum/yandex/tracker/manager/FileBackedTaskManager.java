package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;
    private static final String HEADER = "id,type,title,status,description,epic,duration,start";

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

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\n', ' ');
    }

    private static String taskToString(Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append(task.getId()).append(',')
                .append(task instanceof Epic ? TaskType.EPIC : (task instanceof Subtask ? TaskType.SUBTASK : TaskType.TASK)).append(',')
                .append(escape(task.getTitle())).append(',')
                .append(task.getStatus()).append(',')
                .append(escape(task.getDescription())).append(',');

        if (task instanceof Subtask) {
            sb.append(((Subtask) task).getEpicId());
        }

        sb.append(',');
        String dur = task.getDuration() == null ? "" : String.valueOf(task.getDuration().toMinutes());
        String start = task.getStartTime() == null ? "" : task.getStartTime().toString();
        sb.append(dur).append(',')
                .append(start);
        return sb.toString();
    }

    private static Task taskFromString(String value) {
        try {
            String[] f = value.split(",", -1);
            if (f.length < 6) {
                throw new ManagerSaveException("Bad CSV line: " + value);
            }

            int id = Integer.parseInt(f[0].trim());
            TaskType type = TaskType.valueOf(f[1].trim());
            String title = f[2];
            Status status = Status.valueOf(f[3].trim());
            String desc = f[4];
            Integer epicId = f[5].isEmpty() ? null : Integer.parseInt(f[5].trim());

            Duration dur = (f.length > 6 && !f[6].isEmpty()) ? Duration.ofMinutes(Long.parseLong(f[6].trim())) : null;
            LocalDateTime start = (f.length > 7 && !f[7].isEmpty()) ? LocalDateTime.parse(f[7].trim()) : null;

            switch (type) {
                case TASK -> {
                    Task t = new Task(id, title, desc);
                    t.setStatus(status);
                    t.setDuration(dur);
                    t.setStartTime(start);
                    return t;
                }
                case SUBTASK -> {
                    if (epicId == null) {
                        throw new ManagerSaveException("Subtask without epicId: " + value);
                    }
                    Subtask s = new Subtask(id, title, desc, epicId);
                    s.setStatus(status);
                    s.setDuration(dur);
                    s.setStartTime(start);
                    return s;
                }
                case EPIC -> {
                    Epic e = new Epic(id, title, desc);
                    e.setStatus(status);
                    return e;
                }
            }
            throw new ManagerSaveException("Unknown type: " + type);
        } catch (RuntimeException e) {
            throw new ManagerSaveException("Failed to parse line: " + value, e);
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

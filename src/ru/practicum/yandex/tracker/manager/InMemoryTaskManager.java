package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Status;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.util.*;

public class InMemoryTaskManager implements TaskManager {

    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    private int nextId = 1;

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    //Task

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }

    @Override
    public int createTask(Task task) {
        task.setId(generateId());
        tasks.put(task.getId(), task);
        return task.getId();
    }

    @Override
    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            tasks.put(task.getId(), task);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        tasks.remove(id);
    }

    //Subtask

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public void deleteAllSubtasks() {
        for (Epic epic : epics.values()) {
            for (int sid : epic.getSubtaskIds()) {
                epic.removeSubtask(sid);
            }
            recalcEpicStatus(epic);
        }
        subtasks.clear();
    }

    @Override
    public int createSubtask(Subtask subtask) {
        subtask.setId(generateId());
        subtasks.put(subtask.getId(), subtask);

        Epic parent = epics.get(subtask.getEpicId());
        parent.addSubtask(subtask.getId());
        recalcEpicStatus(parent);

        return subtask.getId();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (!subtasks.containsKey(subtask.getId())) {
            return;
        }
        subtasks.put(subtask.getId(), subtask);
        Epic parent = epics.get(subtask.getEpicId());
        recalcEpicStatus(parent);
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask removed = subtasks.remove(id);
        if (removed != null) {
            Epic parent = epics.get(removed.getEpicId());
            parent.removeSubtask(id);
            recalcEpicStatus(parent);
        }
    }

    //Epic

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public void deleteAllEpics() {
        epics.clear();
        subtasks.clear();
    }

    @Override
    public int createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
        epic.setStatus(Status.NEW);
        return epic.getId();
    }

    @Override
    public void updateEpic(Epic epic) {
        if (!epics.containsKey(epic.getId())) {
            return;
        }
        Epic existing = epics.get(epic.getId());
        existing.setTitle(epic.getTitle());
        existing.setDescription(epic.getDescription());
        recalcEpicStatus(existing);
    }

    @Override
    public void deleteEpicById(int id) {
        Epic removed = epics.remove(id);
        if (removed != null) {
            for (int sid : removed.getSubtaskIds()) {
                subtasks.remove(sid);
            }
        }
    }

    @Override
    public List<Subtask> getSubtasksByEpic(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return Collections.emptyList();
        }
        List<Subtask> list = new ArrayList<>();
        for (int sid : epic.getSubtaskIds()) {
            list.add(subtasks.get(sid));
        }
        return list;
    }

    private int generateId() {
        return nextId++;
    }

    private void recalcEpicStatus(Epic epic) {
        List<Integer> subs = epic.getSubtaskIds();
        if (subs.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;
        for (int sid : subs) {
            Status s = subtasks.get(sid).getStatus();
            if (s != Status.NEW) {
                allNew = false;
            }
            if (s != Status.DONE) {
                allDone = false;
            }
        }

        if (allNew) {
            epic.setStatus(Status.NEW);
        } else if (allDone) {
            epic.setStatus(Status.DONE);
        } else {
            epic.setStatus(Status.IN_PROGRESS);
        }
    }
}

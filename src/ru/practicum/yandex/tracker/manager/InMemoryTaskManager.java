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

    private int nextId = 1;

    private int generateId() {
        return nextId++;
    }

    private void recalcEpicStatus(Epic epic) {
        List<Integer> subs = epic.getSubtaskIds();
        if (subs.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }
        boolean allNew  = true;
        boolean allDone = true;
        for (int sid : subs) {
            Status s = subtasks.get(sid).getStatus();
            if (s != Status.NEW)  allNew  = false;
            if (s != Status.DONE) allDone = false;
        }
        if (allNew) {
            epic.setStatus(Status.NEW);
        } else if (allDone) {
            epic.setStatus(Status.DONE);
        } else {
            epic.setStatus(Status.IN_PROGRESS);
        }
    }

    //Task

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public Task getTaskById(int id) {
        return tasks.get(id);
    }

    @Override
    public void deleteAllTasks() {
        tasks.clear();
    }

    @Override
    public int createTask(Task task) {
        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
        return id;
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
        return subtasks.get(id);
    }

    @Override
    public void deleteAllSubtasks() {
        for (Epic epic : epics.values()) {
            epic.getSubtaskIds().forEach(epic::removeSubtask);
            recalcEpicStatus(epic);
        }
        subtasks.clear();
    }

    @Override
    public int createSubtask(Subtask subtask) {
        int id = generateId();
        subtask.setId(id);
        subtasks.put(id, subtask);

        Epic parent = epics.get(subtask.getEpicId());
        parent.addSubtask(id);
        recalcEpicStatus(parent);

        return id;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        int id = subtask.getId();
        if (!subtasks.containsKey(id)) return;
        subtasks.put(id, subtask);
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
        return epics.get(id);
    }

    @Override
    public void deleteAllEpics() {
        epics.clear();
        subtasks.clear();
    }

    @Override
    public int createEpic(Epic epic) {
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);
        // без подзадач — статус NEW
        epic.setStatus(Status.NEW);
        return id;
    }

    @Override
    public void updateEpic(Epic epic) {
        int id = epic.getId();
        if (!epics.containsKey(id)) return;
        Epic existing = epics.get(id);
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

    //Доп. метод

    @Override
    public List<Subtask> getSubtasksByEpic(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return Collections.emptyList();

        List<Subtask> list = new ArrayList<>();
        for (int sid : epic.getSubtaskIds()) {
            list.add(subtasks.get(sid));
        }
        return list;
    }
}

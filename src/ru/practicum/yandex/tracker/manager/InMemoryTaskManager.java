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

    protected int nextId = 1;

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
        List<Integer> ids = new ArrayList<>(tasks.keySet());
        for (int i = 0; i < ids.size(); i++) {
            historyManager.remove(ids.get(i));
        }
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
        Task removed = tasks.remove(id);
        if (removed != null) {
            historyManager.remove(id);
        }
    }

    protected void putTaskDirect(Task task) {
        tasks.put(task.getId(), task);
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
        List<Integer> ids = new ArrayList<>(subtasks.keySet());
        for (int i = 0; i < ids.size(); i++) {
            historyManager.remove(ids.get(i));
        }
        for (Epic e : epics.values()) {
            List<Integer> sids = e.getSubtaskIds();
            for (int i = 0; i < sids.size(); i++) {
                e.removeSubtask(sids.get(i));
            }
            recalcEpicStatus(e);
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
            if (parent != null) {
                parent.removeSubtask(id);
                recalcEpicStatus(parent);
            }
            historyManager.remove(id);
        }
    }

    protected void putSubtaskDirect(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
        Epic parent = epics.get(subtask.getEpicId());
        if (parent != null) {
            parent.addSubtask(subtask.getId());
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
        for (Epic e : epics.values()) {
            List<Integer> sids = e.getSubtaskIds();
            for (int i = 0; i < sids.size(); i++) {
                historyManager.remove(sids.get(i));
            }
            historyManager.remove(e.getId());
        }
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
            List<Integer> sids = removed.getSubtaskIds();
            for (int i = 0; i < sids.size(); i++) {
                int sid = sids.get(i);
                subtasks.remove(sid);
                historyManager.remove(sid);
            }
            historyManager.remove(id);
        }
    }

    @Override
    public List<Subtask> getSubtasksByEpic(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return Collections.emptyList();
        }
        List<Integer> ids = epic.getSubtaskIds();
        List<Subtask> list = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            Subtask s = subtasks.get(ids.get(i));
            if (s != null) {
                list.add(s);
            }
        }
        return list;
    }

    protected void putEpicDirect(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    protected void setNextIdAfterLoad(int next) {
        this.nextId = next;
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

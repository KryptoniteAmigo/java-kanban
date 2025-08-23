package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Status;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {

    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    private final NavigableSet<Task> prioritized = new TreeSet<>((a, b) -> {
        LocalDateTime sa = a.getStartTime();
        LocalDateTime sb = b.getStartTime();
        if (sa == null && sb == null) return Integer.compare(a.getId(), b.getId());
        if (sa == null) return 1;
        if (sb == null) return -1;
        int cmp = sa.compareTo(sb);
        return (cmp != 0) ? cmp : Integer.compare(a.getId(), b.getId());
    });

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
            return copy(task);
        }
        return task;
    }

    @Override
    public void deleteAllTasks() {
        for (Task t : tasks.values()) {
            if (t.getStartTime() != null) {
                prioritized.remove(t);
            }
            historyManager.remove(t.getId());
        }
        tasks.clear();
    }

    @Override
    public int createTask(Task task) {
        validateNoOverlap(task);
        task.setId(generateId());
        tasks.put(task.getId(), task);
        if (task.getStartTime() != null) {
            prioritized.add(task);
        }
        return task.getId();
    }

    @Override
    public void updateTask(Task task) {
        if (!tasks.containsKey(task.getId())) {
            return;
        }
        validateNoOverlap(task);
        Task old = tasks.get(task.getId());
        if (old != null && old.getStartTime() != null) {
            prioritized.remove(old);
        }
        tasks.put(task.getId(), task);
        if (task.getStartTime() != null) {
            prioritized.add(task);
        }
    }

    @Override
    public void deleteTaskById(int id) {
        Task removed = tasks.remove(id);
        if (removed != null && removed.getStartTime() != null) {
            prioritized.remove(removed);
        }
        if (removed != null) {
            historyManager.remove(id);
        }
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritized);
    }

    protected void putTaskDirect(Task task) {
        tasks.put(task.getId(), task);
        if (task.getStartTime() != null) {
            prioritized.add(task);
        }
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
            return copy(subtask);
        }
        return subtask;
    }

    @Override
    public void deleteAllSubtasks() {
        for (Integer sid : new ArrayList<>(subtasks.keySet())) {
            historyManager.remove(sid);
        }
        prioritized.removeIf(t -> t instanceof Subtask);
        for (Epic epic : epics.values()) {
            for (Integer sid : new ArrayList<>(epic.getSubtaskIds())) {
                epic.removeSubtask(sid);
            }
            recalcEpicStatusAndTime(epic);
        }
        subtasks.clear();
    }

    @Override
    public int createSubtask(Subtask subtask) {
        Epic parent = epics.get(subtask.getEpicId());
        if (parent == null) {
            throw new IllegalArgumentException("Epic " + subtask.getEpicId() + " not found");
        }
        validateNoOverlap(subtask);
        subtask.setId(generateId());
        subtasks.put(subtask.getId(), subtask);
        parent.addSubtask(subtask.getId());
        if (subtask.getStartTime() != null) {
            prioritized.add(subtask);
        }
        recalcEpicStatusAndTime(parent);
        return subtask.getId();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (!subtasks.containsKey(subtask.getId())) {
            return;
        }
        validateNoOverlap(subtask);
        Subtask old = subtasks.get(subtask.getId());
        if (old != null && old.getStartTime() != null) {
            prioritized.remove(old);
        }

        subtasks.put(subtask.getId(), subtask);
        if (subtask.getStartTime() != null) {
            prioritized.add(subtask);
        }

        Epic parent = epics.get(subtask.getEpicId());
        if (parent != null) {
            recalcEpicStatusAndTime(parent);
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask removed = subtasks.remove(id);
        if (removed != null) {
            if (removed.getStartTime() != null) {
                prioritized.remove(removed);
            }
            Epic parent = epics.get(removed.getEpicId());
            if (parent != null) {
                parent.removeSubtask(id);
                recalcEpicStatusAndTime(parent);
            }
            historyManager.remove(id);
        }
    }

    protected void putSubtaskDirect(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
        Epic parent = epics.get(subtask.getEpicId());
        if (parent != null) {
            parent.addSubtask(subtask.getId());
            recalcEpicStatusAndTime(parent);
        }
        if (subtask.getStartTime() != null) {
            prioritized.add(subtask);
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
            return copy(epic);
        }
        return epic;
    }

    @Override
    public void deleteAllEpics() {
        for (Epic e : epics.values()) {
            for (Integer sid : e.getSubtaskIds()) {
                historyManager.remove(sid);
            }
            historyManager.remove(e.getId());
        }
        prioritized.removeIf(t -> t instanceof Subtask);
        epics.clear();
        subtasks.clear();
    }

    @Override
    public int createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
        recalcEpicStatusAndTime(epic);
        return epic.getId();
    }

    @Override
    public void updateEpic(Epic epic) {
        if (!epics.containsKey(epic.getId())) return;
        Epic existing = epics.get(epic.getId());
        existing.setTitle(epic.getTitle());
        existing.setDescription(epic.getDescription());
        recalcEpicStatusAndTime(existing);
    }

    @Override
    public void deleteEpicById(int id) {
        Epic removed = epics.remove(id);
        if (removed != null) {
            for (Integer sid : new ArrayList<>(removed.getSubtaskIds())) {
                Subtask s = subtasks.remove(sid);
                if (s != null && s.getStartTime() != null) {
                    prioritized.remove(s);
                }
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
        recalcEpicStatusAndTime(epic);
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

    private void validateNoOverlap(Task candidate) {
        if (candidate.getStartTime() == null || candidate.getDuration() == null) {
            return;
        }
        Task left = prioritized.lower(candidate);
        Task right = prioritized.higher(candidate);
        if (overlaps(candidate, left) || overlaps(candidate, right)) {
            throw new IllegalArgumentException("Время выполнения задач совпадает");
        }
    }

    private static boolean overlaps(Task a, Task b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() == b.getId()) {
            return false;
        }
        if (a.getStartTime() == null || b.getStartTime() == null) {
            return false;
        }
        LocalDateTime aEnd = a.getEndTime();
        LocalDateTime bEnd = b.getEndTime();
        return aEnd != null && bEnd != null
                && a.getStartTime().isBefore(bEnd)
                && b.getStartTime().isBefore(aEnd);
    }

    private void recalcEpicStatusAndTime(Epic epic) {
        List<Integer> ids = epic.getSubtaskIds();

        if (ids.isEmpty()) {
            epic.setStatus(Status.NEW);
            epic.setDuration(Duration.ZERO);
            epic.setStartTime(null);
            epic.setEndTime(null);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;
        Duration total = Duration.ZERO;
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;

        for (int sid : ids) {
            Subtask s = subtasks.get(sid);
            if (s == null) {
                continue;
            }
            if (s.getStatus() != Status.NEW) {
                allNew = false;
            }
            if (s.getStatus() != Status.DONE) {
                allDone = false;
            }
            if (s.getDuration() != null) {
                total = total.plus(s.getDuration());
            }
            LocalDateTime st = s.getStartTime();
            LocalDateTime en = s.getEndTime();
            if (st != null && (minStart == null || st.isBefore(minStart))) {
                minStart = st;
            }
            if (en != null && (maxEnd == null || en.isAfter(maxEnd))) {
                maxEnd = en;
            }
        }

        epic.setStatus(allNew ? Status.NEW : (allDone ? Status.DONE : Status.IN_PROGRESS));
        epic.setDuration(total);
        epic.setStartTime(minStart);
        epic.setEndTime(maxEnd);
    }

    private Task copy(Task t) {
        if (t == null) return null;
        Task c = new Task(t.getId(), t.getTitle(), t.getDescription());
        c.setStatus(t.getStatus());
        c.setDuration(t.getDuration());
        c.setStartTime(t.getStartTime());
        return c;
    }

    private Subtask copy(Subtask s) {
        if (s == null) return null;
        Subtask c = new Subtask(s.getId(), s.getTitle(), s.getDescription(), s.getEpicId());
        c.setStatus(s.getStatus());
        c.setDuration(s.getDuration());
        c.setStartTime(s.getStartTime());
        return c;
    }

    private Epic copy(Epic e) {
        if (e == null) return null;
        Epic c = new Epic(e.getId(), e.getTitle(), e.getDescription());
        c.setStatus(e.getStatus());
        c.setDuration(e.getDuration());
        c.setStartTime(e.getStartTime());
        c.setEndTime(e.getEndTime());
        return c;
    }
}

package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.Task;
import java.util.ArrayList;
import java.util.List;

public class InMemoryHistoryManager implements HistoryManager {
    private final List<Task> history = new ArrayList<>();

    @Override
    public void add(Task task) {
        Task snapshot = new Task(task.getId(), task.getTitle(), task.getDescription());
        snapshot.setStatus(task.getStatus());

        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getId() == task.getId()) {
                history.remove(i);
                break;
            }
        }
        history.add(snapshot);
        if (history.size() > 10) {
            history.remove(0);
        }
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}

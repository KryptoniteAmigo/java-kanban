package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.Task;
import java.util.List;

public interface HistoryManager {

    void add(Task task);

    void remove(int id);

    List<Task> getHistory();
}

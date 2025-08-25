package ru.practicum.yandex.tracker.manager;

public class Managers {
    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }

    public static FileBackedTaskManager getFileBacked(java.io.File file) {
        return new FileBackedTaskManager(file);
    }
}

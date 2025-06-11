package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.util.List;

public interface TaskManager {

    List<Task> getHistory();

    List<Task> getAllTasks();
    Task getTaskById(int id);
    void deleteAllTasks();
    int  createTask(Task task);
    void updateTask(Task task);
    void deleteTaskById(int id);

    List<Subtask> getAllSubtasks();
    Subtask getSubtaskById(int id);
    void deleteAllSubtasks();
    int createSubtask(Subtask subtask);
    void updateSubtask(Subtask subtask);
    void deleteSubtaskById(int id);

    List<Epic> getAllEpics();
    Epic getEpicById(int id);
    void deleteAllEpics();
    int createEpic(Epic epic);
    void updateEpic(Epic epic);
    void deleteEpicById(int id);

    List<Subtask> getSubtasksByEpic(int epicId);
}
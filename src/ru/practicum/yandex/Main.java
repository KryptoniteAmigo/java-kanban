package ru.practicum.yandex;

import ru.practicum.yandex.tracker.manager.InMemoryTaskManager;
import ru.practicum.yandex.tracker.manager.TaskManager;
import ru.practicum.yandex.tracker.model.Epic;
import ru.practicum.yandex.tracker.model.Status;
import ru.practicum.yandex.tracker.model.Subtask;
import ru.practicum.yandex.tracker.model.Task;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final TaskManager manager = new InMemoryTaskManager();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": createTask();       break;
                case "2": getTaskById();      break;
                case "3": listAllTasks();     break;
                case "4": updateTask();       break;
                case "5": deleteTaskById();   break;
                case "6": deleteAllTasks();   break;

                case "7": createEpic();       break;
                case "8": getEpicById();      break;
                case "9": listAllEpics();     break;
                case "10": updateEpic();      break;
                case "11": deleteEpicById();  break;
                case "12": deleteAllEpics();  break;

                case "13": createSubtask();      break;
                case "14": getSubtaskById();     break;
                case "15": listAllSubtasks();    break;
                case "16": listSubtasksByEpic(); break;
                case "17": updateSubtask();      break;
                case "18": deleteSubtaskById();  break;
                case "19": deleteAllSubtasks();  break;

                case "0": running = false;       break;
                default:
                    System.out.println("Некорректный ввод, попробуйте снова.");
            }
        }
        System.out.println("До встречи!");
    }

    private static void printMenu() {
        System.out.println("\nТрекер задач");
        System.out.println("1.  Создать Task");
        System.out.println("2.  Показать Task по ID");
        System.out.println("3.  Показать все Tasks");
        System.out.println("4.  Обновить Task");
        System.out.println("5.  Удалить Task по ID");
        System.out.println("6.  Удалить все Tasks");
        System.out.println("7.  Создать Epic");
        System.out.println("8.  Показать Epic по ID");
        System.out.println("9.  Показать все Epics");
        System.out.println("10. Обновить Epic");
        System.out.println("11. Удалить Epic по ID");
        System.out.println("12. Удалить все Epics");
        System.out.println("13. Создать Subtask");
        System.out.println("14. Показать Subtask по ID");
        System.out.println("15. Показать все Subtasks");
        System.out.println("16. Показать Subtasks выбранного Epic");
        System.out.println("17. Обновить Subtask");
        System.out.println("18. Удалить Subtask по ID");
        System.out.println("19. Удалить все Subtasks");
        System.out.println("0.  Выход");
        System.out.print("Выберите пункт: ");
    }

    private static void createTask() {
        System.out.print("Название Task: ");
        String title = scanner.nextLine();
        System.out.print("Описание: ");
        String desc = scanner.nextLine();
        Task task = new Task(0, title, desc);
        int id = manager.createTask(task);
        System.out.println("Task создан с ID=" + id);
    }

    private static void getTaskById() {
        System.out.print("ID Task: ");
        int id = Integer.parseInt(scanner.nextLine());
        Task t = manager.getTaskById(id);
        if (t == null) {
            System.out.println("Task с ID=" + id + " не найден.");
        } else {
            System.out.printf("ID=%d | %s | %s | %s%n",
                    t.getId(), t.getTitle(), t.getStatus(), t.getDescription());
        }
    }

    private static void listAllTasks() {
        List<Task> tasks = manager.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("Нет созданных Tasks.");
        } else {
            for (Task t : tasks) {
                System.out.printf("ID=%d | %s | %s | %s%n",
                        t.getId(), t.getTitle(), t.getStatus(), t.getDescription());
            }
        }
    }

    private static void updateTask() {
        System.out.print("ID Task для обновления: ");
        int id = Integer.parseInt(scanner.nextLine());
        Task t = manager.getTaskById(id);
        if (t == null) {
            System.out.println("Task не найден.");
            return;
        }
        System.out.print("Новое название (ENTER чтобы оставить): ");
        String title = scanner.nextLine().trim();
        if (!title.isEmpty()) t.setTitle(title);

        System.out.print("Новое описание (ENTER чтобы оставить): ");
        String desc = scanner.nextLine().trim();
        if (!desc.isEmpty()) t.setDescription(desc);

        System.out.print("Новый статус (NEW, IN_PROGRESS, DONE, ENTER чтобы оставить): ");
        String ss = scanner.nextLine().trim().toUpperCase();
        if (!ss.isEmpty()) {
            try {
                t.setStatus(Status.valueOf(ss));
            } catch (IllegalArgumentException e) {
                System.out.println("Некорректный статус, оставлен прежний.");
            }
        }

        manager.updateTask(t);
        System.out.println("Task обновлён.");
    }

    private static void deleteTaskById() {
        System.out.print("ID Task для удаления: ");
        int id = Integer.parseInt(scanner.nextLine());
        manager.deleteTaskById(id);
        System.out.println("Task удалён (если существовал).");
    }

    private static void deleteAllTasks() {
        manager.deleteAllTasks();
        System.out.println("Все Tasks удалены.");
    }

    private static void createEpic() {
        System.out.print("Название Epic: ");
        String title = scanner.nextLine();
        System.out.print("Описание: ");
        String desc = scanner.nextLine();
        Epic epic = new Epic(0, title, desc);
        int id = manager.createEpic(epic);
        System.out.println("Epic создан с ID=" + id);
    }

    private static void getEpicById() {
        System.out.print("ID Epic: ");
        int id = Integer.parseInt(scanner.nextLine());
        Epic e = manager.getEpicById(id);
        if (e == null) {
            System.out.println("Epic с ID=" + id + " не найден.");
        } else {
            System.out.printf("ID=%d | %s | %s | Подзадач: %s%n",
                    e.getId(), e.getTitle(), e.getStatus(), e.getSubtaskIds());
        }
    }

    private static void listAllEpics() {
        List<Epic> epics = manager.getAllEpics();
        if (epics.isEmpty()) {
            System.out.println("Нет созданных Epics.");
        } else {
            for (Epic e : epics) {
                System.out.printf("ID=%d | %s | %s | Подзадач: %s%n",
                        e.getId(), e.getTitle(), e.getStatus(), e.getSubtaskIds());
            }
        }
    }

    private static void updateEpic() {
        System.out.print("ID Epic для обновления: ");
        int id = Integer.parseInt(scanner.nextLine());
        Epic e = manager.getEpicById(id);
        if (e == null) {
            System.out.println("Epic не найден.");
            return;
        }
        System.out.print("Новое название (ENTER чтобы оставить): ");
        String title = scanner.nextLine().trim();
        if (!title.isEmpty()) e.setTitle(title);

        System.out.print("Новое описание (ENTER чтобы оставить): ");
        String desc = scanner.nextLine().trim();
        if (!desc.isEmpty()) e.setDescription(desc);

        manager.updateEpic(e);
        System.out.println("Epic обновлён (статус может быть пересчитан).");
    }

    private static void deleteEpicById() {
        System.out.print("ID Epic для удаления: ");
        int id = Integer.parseInt(scanner.nextLine());
        manager.deleteEpicById(id);
        System.out.println("Epic и его Subtasks удалены (если существовали).");
    }

    private static void deleteAllEpics() {
        manager.deleteAllEpics();
        System.out.println("Все Epics (и Subtasks) удалены.");
    }

    private static void createSubtask() {
        System.out.print("ID родительского Epic: ");
        int epicId = Integer.parseInt(scanner.nextLine());
        Epic parent = manager.getEpicById(epicId);
        if (parent == null) {
            System.out.println("Epic с таким ID не найден.");
            return;
        }
        System.out.print("Название Subtask: ");
        String title = scanner.nextLine();
        System.out.print("Описание: ");
        String desc = scanner.nextLine();
        Subtask sub = new Subtask(0, title, desc, epicId);
        int id = manager.createSubtask(sub);
        System.out.println("Subtask создан с ID=" + id);
    }

    private static void getSubtaskById() {
        System.out.print("ID Subtask: ");
        int id = Integer.parseInt(scanner.nextLine());
        Subtask s = manager.getSubtaskById(id);
        if (s == null) {
            System.out.println("Subtask с ID=" + id + " не найден.");
        } else {
            System.out.printf("ID=%d | %s | %s | %s | EpicID=%d%n",
                    s.getId(), s.getTitle(), s.getStatus(), s.getDescription(), s.getEpicId());
        }
    }

    private static void listAllSubtasks() {
        List<Subtask> subs = manager.getAllSubtasks();
        if (subs.isEmpty()) {
            System.out.println("Нет созданных Subtasks.");
        } else {
            subs.forEach(s ->
                    System.out.printf("ID=%d | %s | %s | %s | EpicID=%d%n",
                            s.getId(), s.getTitle(), s.getStatus(), s.getDescription(), s.getEpicId())
            );
        }
    }

    private static void listSubtasksByEpic() {
        System.out.print("ID Epic для Subtasks: ");
        int epicId = Integer.parseInt(scanner.nextLine());
        List<Subtask> subs = manager.getSubtasksByEpic(epicId);
        if (subs.isEmpty()) {
            System.out.println("Для этого Epic нет Subtasks или Epic не найден.");
        } else {
            for (Subtask s : subs) {
                System.out.printf("ID=%d | %s | %s | %s%n",
                        s.getId(), s.getTitle(), s.getStatus(), s.getDescription());
            }
        }
    }

    private static void updateSubtask() {
        System.out.print("ID Subtask для обновления: ");
        int id = Integer.parseInt(scanner.nextLine());
        Subtask s = manager.getSubtaskById(id);
        if (s == null) {
            System.out.println("Subtask не найден.");
            return;
        }
        System.out.print("Новое название (ENTER чтобы оставить): ");
        String title = scanner.nextLine().trim();
        if (!title.isEmpty()) s.setTitle(title);

        System.out.print("Новое описание (ENTER чтобы оставить): ");
        String desc = scanner.nextLine().trim();
        if (!desc.isEmpty()) s.setDescription(desc);

        System.out.print("Новый статус (NEW, IN_PROGRESS, DONE, ENTER чтобы оставить): ");
        String ss = scanner.nextLine().trim().toUpperCase();
        if (!ss.isEmpty()) {
            try {
                s.setStatus(Status.valueOf(ss));
            } catch (IllegalArgumentException e) {
                System.out.println("Некорректный статус, оставлен прежний.");
            }
        }

        manager.updateSubtask(s);
        System.out.println("- Subtask обновлён.");
    }

    private static void deleteSubtaskById() {
        System.out.print("ID Subtask для удаления: ");
        int id = Integer.parseInt(scanner.nextLine());
        manager.deleteSubtaskById(id);
        System.out.println("- Subtask удалён (если существовал).");
    }

    private static void deleteAllSubtasks() {
        manager.deleteAllSubtasks();
        System.out.println("- Все Subtasks удалены.");
    }
}
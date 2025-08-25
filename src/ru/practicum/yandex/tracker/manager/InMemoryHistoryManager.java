package ru.practicum.yandex.tracker.manager;

import ru.practicum.yandex.tracker.model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    private final Map<Integer, Node> index = new HashMap<>();
    private Node head;
    private Node tail;

    @Override
    public void add(Task task) {
        if (task == null) return;

        Node old = index.remove(task.getId());
        if (old != null) {
            removeNode(old);
        }

        Task snapshot = new Task(task.getId(), task.getTitle(), task.getDescription());
        snapshot.setStatus(task.getStatus());

        Node node = linkLast(snapshot);
        index.put(snapshot.getId(), node);
    }

    @Override
    public void remove(int id) {
        Node node = index.remove(id);
        if (node != null) {
            removeNode(node);
        }
    }

    @Override
    public List<Task> getHistory() {
        List<Task> list = new ArrayList<>();
        Node cur = head;
        while (cur != null) {
            list.add(cur.task);
            cur = cur.next;
        }
        return list;
    }

    private static class Node {
        Task task;
        Node prev;
        Node next;

        Node(Task task) {
            this.task = task;
        }
    }

    private Node linkLast(Task t) {
        Node node = new Node(t);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }
        return node;
    }

    private void removeNode(Node node) {
        if (node == null) return;
        Node p = node.prev;
        Node n = node.next;
        if (p != null) {
            p.next = n;
        } else {
            head = n;
        }
        if (n != null) {
            n.prev = p;
        } else {
            tail = p;
        }
        node.prev = null;
        node.next = null;
    }
}

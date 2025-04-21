import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

class Task {
    private final int id;
    public Task(int id) { this.id = id; }
    public int getId() { return id; }
}

class SharedQueue {
    private final Queue<Task> taskQueue = new LinkedList<>();
    private final Lock lock = new ReentrantLock();

    public void addTask(Task task) {
        lock.lock();
        try {
            taskQueue.add(task);
        } finally {
            lock.unlock();
        }
    }

    public Task getTask() {
        lock.lock();
        try {
            return taskQueue.poll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return taskQueue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}

class Worker implements Runnable {
    private final int id;
    private final SharedQueue queue;
    private final BufferedWriter writer;

    public Worker(int id, SharedQueue queue, BufferedWriter writer) {
        this.id = id;
        this.queue = queue;
        this.writer = writer;
    }

    @Override
    public void run() {
        System.out.println("Worker " + id + " started.");
        while (true) {
            Task task = queue.getTask();
            if (task == null) break;
            try {
                Thread.sleep(100); // simulate work
                String result = "Worker " + id + " processed Task " + task.getId();
                System.out.println(result);
                synchronized (writer) {
                    writer.write(result + "\n");
                }
            } catch (InterruptedException | IOException e) {
                System.err.println("Worker " + id + " error: " + e.getMessage());
            }
        }
        System.out.println("Worker " + id + " finished.");
    }
}

public class RideSharingSystem {
    public static void main(String[] args) {
        SharedQueue queue = new SharedQueue();
        int totalTasks = 20;
        int workerCount = 5;

        // Fill the queue
        for (int i = 1; i <= totalTasks; i++) {
            queue.addTask(new Task(i));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"))) {
            ExecutorService executor = Executors.newFixedThreadPool(workerCount);
            for (int i = 1; i <= workerCount; i++) {
                executor.execute(new Worker(i, queue, writer));
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (IOException | InterruptedException e) {
            System.err.println("Main thread error: " + e.getMessage());
        }
    }
}
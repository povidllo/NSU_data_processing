package kuzminov;

import java.util.concurrent.*;

public class MyThreadPool {

    private final BlockingQueue<Runnable> tasks;

    public MyThreadPool(int threadsCount) {
        this.tasks = new LinkedBlockingQueue<>();
        Thread[] threads = new Thread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            ThreadFromPool runnableThread = new ThreadFromPool();
            Thread thread = new Thread(runnableThread);
            threads[i] = thread;
            thread.start();
        }
    }

    public void submit(Runnable task) throws InterruptedException {
        tasks.put(task);
    }

    public <V> Future<V> submit(Callable<V> task) {
        FutureTask<V> future = new FutureTask<>(task);
        try {
            tasks.put(future);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return future;
    }

    class ThreadFromPool implements Runnable{

        @Override
        public void run() {
            while (true) {
                try {
                    Runnable task = tasks.take();
                    task.run();
                } catch (InterruptedException e) {
                    break;
                }
            }

        }
    }
}

package kuzminov;

import java.util.*;
import java.io.*;

class SynchronizedList {
    private final List<String> list;
    private final int delayMs;
    private final int threadCount;
    private final List<Thread> threads = new ArrayList<>();
    private volatile boolean running = true;
    private int stepCounter = 0;

    public SynchronizedList(List<String> list, int delayMs, int threadCount) {
        this.list = list;
        this.delayMs = delayMs;
        this.threadCount = threadCount;
    }

    public void startSorting() {
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread t = new Thread(() -> bubbleSortForever(threadId), "Sorter-" + threadId);
            t.start();
            threads.add(t);
        }
    }

    private void bubbleSortForever(int threadId) {
        try {
            while (running) {
                boolean swapped = false;

                for (int i = 0; i < list.size() - 1; i++) {
                    synchronized (list) {
                        if (i + 1 >= list.size()) continue;
                        String a = list.get(i);
                        String b = list.get(i + 1);
                        stepCounter++;

                        System.out.printf("[Thread-%d] compare: \"%s\" и \"%s\"%n",
                                threadId, a, b);

                        if (a.compareTo(b) > 0) {
                            list.set(i, b);
                            list.set(i + 1, a);
                            swapped = true;

                            System.out.printf("[Thread-%d] swap elements: \"%s\" <-> \"%s\"%n",
                                    threadId, a, b);
                        }
                    }

                    Thread.sleep(delayMs);
                }

                if (!swapped) {
                    Thread.sleep(delayMs);
                }
            }
        } catch (InterruptedException ignored) {}
    }

    public void stop() {
        running = false;
        for (Thread t : threads) t.interrupt();
    }

    public int getStepCounter() {
        return stepCounter;
    }

    public List<String> getListSnapshot() {
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public static void printList(List<String> list) {
        synchronized (list) {
            for (String s : list) {
                System.out.println(s);
            }
        }
    }
}



class MainA {
    public static void main(String[] args) {
        List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>());

        SynchronizedList sorter = new SynchronizedList(synchronizedList, 1000, 3);
        sorter.startSorting();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (true) {
                line = reader.readLine();
                if (line == null) break;

                if (line.isEmpty()) {
                    System.out.println("----- Current list state -----");
                    SynchronizedList.printList(sorter.getListSnapshot());
                    System.out.println("------------------------------------");
                    continue;
                }

                while (line.length() > 80) {
                    String part = line.substring(0, 80);
                    synchronizedList.add(0, part);
                    line = line.substring(80);
                }
                synchronizedList.add(0, line);
            }

        } catch (IOException e) {
            System.err.println("Ошибка чтения ввода: " + e.getMessage());
        }

        sorter.stop();
    }
}


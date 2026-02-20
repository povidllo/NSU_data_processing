package kuzminov;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Scanner;
import java.lang.Thread;

public class Main {
    static class Node {
        String value;
        Node next;
        Node prev;
        ReentrantLock lock = new ReentrantLock();

        Node(String value) {
            this.value = value;
        }
    }

    static class MyLinkedList implements Iterable<String> {
        private Node dummy = new Node(null);

        public MyLinkedList() {
            dummy.next = null;
            dummy.prev = null;
        }

        public void addFirst(String value) {
            Node newNode = new Node(value);
            dummy.lock.lock();
            try {
                newNode.next = dummy.next;
                if (dummy.next != null) {
                    dummy.next.prev = newNode;
                }
                newNode.prev = dummy;
                dummy.next = newNode;
            } finally {
                dummy.lock.unlock();
            }
        }

        public void bubbleSort() throws InterruptedException {
            int step = 0;
            Node prev = dummy;
            prev.lock.lock();
            Node current = prev.next;
            if (current == null) {
                prev.lock.unlock();
                return;
            }
            current.lock.lock();
            try {
                while (current.next != null) {
                    Node nextNode = current.next;
                    nextNode.lock.lock();
                    step++;
                    if (current.value.compareTo(nextNode.value) > 0) {
                        prev.next = nextNode;
                        nextNode.prev = prev;
                        current.next = nextNode.next;
                        if (nextNode.next != null) {
                            nextNode.next.prev = current;
                        }
                        nextNode.next = current;
                        current.prev = nextNode;
                        prev.lock.unlock();
                        prev = nextNode;
                    } else {
                        prev.lock.unlock();
                        prev = current;
                        current = nextNode;
                    }
//                    Thread.sleep(1000);
                }
            } finally {
                current.lock.unlock();
                prev.lock.unlock();
            }
//            Thread.sleep(1000);
//            System.out.println(step);
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                Node current = dummy.next;

                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public String next() {
                    if (current == null) {
                        throw new NoSuchElementException();
                    }
                    String value = current.value;
                    current = current.next;
                    return value;
                }
            };
        }
    }

    static class Sorter implements Runnable {
        private MyLinkedList list;

        Sorter(MyLinkedList list) {
            this.list = list;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    list.bubbleSort();
//                    System.out.println("прошел");
//                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        MyLinkedList list = new MyLinkedList();

        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new Sorter(list));
            threads[i].start();
        }

        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                for (String s : list) {
                    System.out.print(s + " ");
                }
                System.out.print("\n");
                return;
            } else {
                list.addFirst(line);
            }
        }
    }
}
package kuzminov;

import java.io.*;
import java.util.*;

public class SynchronizedListTest {
    private static final int RUNS = 3;
    private static final int DELAY_MS = 500;
    private static final int THREADS = 3;
    private static final int SORT_TIME_MS = 30000;

    public static void main(String[] args) {
        for (int run = 1; run <= RUNS; run++) {
            System.out.printf("=== Запуск %d/%d ===%n", run, RUNS);

            List<String> synchronizedList = Collections.synchronizedList(new ArrayList<>());

            try (BufferedReader reader = new BufferedReader(new FileReader("test.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;

                    // разбиваем длинные строки
                    while (line.length() > 80) {
                        String part = line.substring(0, 80);
                        synchronizedList.add(0, part);
                        line = line.substring(80);
                    }
                    synchronizedList.add(0, line);
                }
            } catch (IOException e) {
                System.err.println("Ошибка чтения test.txt: " + e.getMessage());
                return;
            }

            SynchronizedList sorter = new SynchronizedList(synchronizedList, DELAY_MS, THREADS);
            sorter.startSorting();

            try {
                System.out.println("[Main] ожидание сортировки...");
                Thread.sleep(SORT_TIME_MS);
            } catch (InterruptedException ignored) {}

            sorter.stop();

            String resultFile = "result_" + run + ".txt";
            List<String> resultSnapshot = sorter.getListSnapshot();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
                for (String s : resultSnapshot) {
                    writer.write(s + "\n");
                }
//                writer.write("\nКоличество шагов: " + sorter.getStepCounter() + "\n");
            } catch (IOException e) {
                System.err.println("Ошибка записи " + resultFile + ": " + e.getMessage());
            }

            System.out.println("-----------------------------");
        }

        compareResults();
    }

    private static void compareResults() {
        System.out.println("=== Сравнение результатов ===");
        try {
            List<String> base = readFile("result_1.txt");
            boolean allEqual = true;

            for (int i = 2; i <= RUNS; i++) {
                List<String> next = readFile("result_" + i + ".txt");
                if (!base.equals(next)) {
                    allEqual = false;
                    System.out.println("Файлы result_1.txt и result_" + i + ".txt отличаются!");
                } else {
                    System.out.println("Файлы result_1.txt и result_" + i + ".txt совпадают");
                }
            }

            if (allEqual) {
                System.out.println("Все результаты одинаковые — сортировка стабильна");
            } else {
                System.out.println("Результаты отличаются — порядок зависит от потоков");
            }

        } catch (IOException e) {
            System.err.println("Ошибка сравнения файлов: " + e.getMessage());
        }
    }

    private static List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Финальное") || line.startsWith("Количество")) continue;
                if (line.trim().isEmpty()) continue;
                lines.add(line);
            }
        }
        return lines;
    }
}

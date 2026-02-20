package kuzminov;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MainTest {
    public static void main(String[] args) throws Exception {
        Path inputFile = Path.of("test.txt");
        var lines = Files.readAllLines(inputFile);

        Path[] outputFiles = new Path[10];

        for (int i = 0; i < 10; i++) {
            Path outputFile = Path.of("sorted_output" + i + ".txt");
            outputFiles[i] = outputFile;

            PrintStream fileOut = new PrintStream(new FileOutputStream(outputFile.toFile()));

            PrintStream oldOut = System.out;
            System.setOut(fileOut);

            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            System.setIn(pis);

            Thread inputThread = new Thread(() -> {
                try (PrintWriter writer = new PrintWriter(pos)) {
                    for (String line : lines) {
                        writer.println(line);
                        writer.flush();
                    }
                    Thread.sleep(5000);
                    writer.println();
                    writer.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            inputThread.start();

            Thread mainThread = new Thread(() -> {
                Main.main(new String[]{});
            });
            mainThread.start();

            mainThread.join();
            inputThread.join();

            fileOut.close();
            System.setOut(oldOut);
            System.out.println("Сортировка " + i + " завершена, результат в " + outputFile);
        }

        boolean allEqual = true;
        for (int i = 0; i < outputFiles.length; i++) {
            List<String> linesI = Files.readAllLines(outputFiles[i]);
            for (int j = i + 1; j < outputFiles.length; j++) {
                List<String> linesJ = Files.readAllLines(outputFiles[j]);
                if (!linesI.equals(linesJ)) {
                    System.out.println("Файлы " + outputFiles[i] + " и " + outputFiles[j] + " отличаются!");
                    allEqual = false;
                }
            }
        }

        if (allEqual) {
            System.out.println("Файлы идентичны");
        } else {
            System.out.println("Файлы отличаются.");
        }
    }
}

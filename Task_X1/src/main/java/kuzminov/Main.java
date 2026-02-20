package kuzminov;

import java.io.File;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        File input = new File("people.xml");
        File output = new File("output.xml");

        Map<String, kuzminov.Person> persons = PersonParser.parse(input);

        PersonParser.writeToFile(persons, output);
    }
}

package kuzminov;

import java.io.File;
import java.util.Map;

public class Main {
    static void main() throws Exception {
        File input = new File("people.xml");
        File output = new File("output-jaxb.xml");
        File schemaFile = new File("people.xsd");

        Map<String, Person> data = PersonParser.parse(input);
        PersonParser.writeToFileWithJAXB(data, output, schemaFile);

        File xml = new File("output-jaxb.xml");
        File xsd = new File("people.xsd");

        XMLValidator.validateXML(xml, xsd);
    }
}

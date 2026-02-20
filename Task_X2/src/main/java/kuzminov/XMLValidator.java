package kuzminov;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;

public class XMLValidator {
    public static void validateXML(File xml, File xsd) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = schemaFactory.newSchema(xsd);
        factory.setSchema(schema);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(xml);

        System.out.println("XML is valid according to XSD");
    }
}

package kuzminov;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.*;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import java.util.*;

public class PersonParser {
    public static Map<String, Person> personsById = new HashMap<>();
    public static Map<String, Person> personsByName = new HashMap<>();

public static Map<String, Person> parse(File file) throws Exception {
    XMLStreamReader r = XMLInputFactory.newInstance()
            .createXMLStreamReader(new FileInputStream(file));

    Person current = null;
    String currentTag = null;

    while (r.hasNext()) {
        int event = r.next();

        switch (event) {
            case XMLStreamConstants.START_ELEMENT -> {
                currentTag = r.getLocalName();

                if ("person".equals(currentTag)) {
                    current = new Person();

                    String id = r.getAttributeValue(null, "id");
                    if (id != null) current.id = id;

                    String name = r.getAttributeValue(null, "name");
                    if (name != null) {
                        String[] splitName = name.trim().split("\\s+");
                        if (splitName.length >= 1) current.firstName = splitName[0];
                        if (splitName.length >= 2) current.surname = splitName[1];
                    }
                }

                if (current != null) {
                    switch (currentTag) {
                        case "id" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.id = val;
                        }
                        case "firstname" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.firstName = val;
                        }
                        case "surname" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.surname = val;
                        }
                        case "gender" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.gender = normalizeGender(val);
                        }
                        case "wife" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.wifeId = val;
                        }
                        case "husband" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.husbandId = val;
                        }
                        case "spouce" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null && !val.equals("NONE")) {
                                String[] newVal = val.trim().split("\\s+");
//                                if (nameHash(newVal[0], newVal[1]).equals(" Claudia")) {
//                                    System.out.println("d");
//                                }
                                current.spouseName = nameHash(newVal[0], newVal[1]);
                            }
                        }
                        case "parent" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null && !val.equals("UNKNOWN")) current.parentsId.add(val);
                        }
                        case "siblings" -> {
                            String val = r.getAttributeValue(null, "val");
                            if (val != null) current.siblingsId.addAll(Arrays.asList(val.trim().split("\\s+")));
                        }
                        case "son" -> {
                            String val = r.getAttributeValue(null, "id");
                            if (val != null) current.sonId.add(val);
                        }
                        case "daughter" -> {
                            String val = r.getAttributeValue(null, "id");
                            if (val != null) current.daughterId.add(val);
                        }
                        case "children-number" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.childrenNumber = Integer.parseInt(val);
                        }
                        case "siblings-number" -> {
                            String val = r.getAttributeValue(null, "value");
                            if (val != null) current.siblingsNumber = Integer.parseInt(val);
                        }
                    }
                }
            }

            case XMLStreamConstants.CHARACTERS -> {
                if (current == null) continue;
                String text = r.getText().trim();
                if (text.isEmpty()) continue;

                switch (currentTag) {
                    case "parent" -> current.parentsId.add(text);
                    case "firstname", "first" -> current.firstName = text;
                    case "gender" -> current.gender = normalizeGender(text);
                    case "family", "family-name", "surname" -> current.surname = text;
                    case "father" -> {
                        String[] newVal = text.trim().split("\\s+");
                        current.fatherName = nameHash(newVal[0], newVal[1]);
                    }
                    case "mother" -> {
                        String[] newVal = text.trim().split("\\s+");

                        current.motherName = nameHash(newVal[0], newVal[1]);
                    }
                    case "child" -> {
                        String[] newVal = text.trim().split("\\s+");

                        current.childrenName.add(nameHash(newVal[0], newVal[1]));
                    }
                    case "sister" -> {
                        String[] newVal = text.trim().split("\\s+");

                        current.sistersName.add(nameHash(newVal[0], newVal[1]));
                    }
                    case "brother" -> {
                        String[] newVal = text.trim().split("\\s+");

                        current.brothersName.add(nameHash(newVal[0], newVal[1]));
                    }
                }
            }

            case XMLStreamConstants.END_ELEMENT -> {
                if ("person".equals(r.getLocalName()) && current != null) {

                    String nameKey = nameHash(current.firstName, current.surname);

//                    if((nameKey != null && nameKey.equals("Lurline Trawick"))) {
//                        System.out.println("f");
//                    }
//                    if((current.id != null && current.id.equals("P405298"))) {
//                        System.out.println("f");
//                    }
                    Person target = null;

                    if(current.id != null && nameKey != null) {
                        target = personsById.get(current.id);
                        Person targetByName = personsByName.get(nameKey);

                        if(target == targetByName && target != null) { //встречаются не впервые и связаны
                            target.union(current);
                        } else if(target == targetByName && target == null) { // до этого не было ни по id, ни по имени
                            target = new Person();
                            target.union(current);
                            personsById.put(current.id, target);
                            personsByName.put(nameKey, target);
                        } else { //встречались имя без id и id без имени
                            if(target == null) {
                                target = new Person();
                            }
                            if(targetByName == null) {
                                targetByName = new Person();
                            }
                            target.union(current);
                            target.union(targetByName);
                            personsByName.put(nameKey, target);
                            personsById.put(current.id, target);
                        }
                    } else if (current.id != null) { // только id
                        target = personsById.get(current.id);
                        if(target == null) { // первый раз встречается
                            target = new Person();
                            target.union(current);
                            personsById.put(current.id, target);
                        } else { //встречается не первый раз и возможно имеется ссвязь с personsByName
                            target.union(current);
                            nameKey = nameHash(target.firstName, target.surname);

                            if(nameKey != null) { //так получилось, что firstname и surname не встречались вметсе
                                Person targetByName = personsByName.get(nameKey);
                                if(targetByName != null && target.id.equals(targetByName.id)) {
                                    target.union(targetByName);
                                }
                                personsByName.put(nameKey, target);
                            }

                        }
                    } else if (nameKey != null) {
                        target = personsByName.get(nameKey);
                        if(target == null) { // первый раз встречается
                            target = new Person();
                            target.union(current);
                            personsByName.put(nameKey, target);
                        } else { //встречается не первый раз и возможно имеется ссвязь с personsById
                            target.union(current);
                        }
                    } else {
                        throw new Exception("чето не то");
                    }
                }
                currentTag = null;
            }
        }
    }
    for (Person p : personsById.values()) p.validate();

    return personsById;
}

    private static String normalizeGender(String g) {
        return switch (g.toUpperCase()) {
            case "M", "MALE" -> "male";
            case "F", "FEMALE" -> "female";
            default -> "";
        };
    }

    private static String nameHash(String f, String l) {
        if(f == null || l == null) {
            return null;
        }
        return f + " " + l;
    }

    public static void writeToFile(Map<String, Person> persons, File file) throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(new FileOutputStream(file), "UTF-8");

        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("people");

        int index = 0;
        for (Person p : persons.values()) {
            index++;
            System.out.println(index);

            if (p.id == null) continue;

            writer.writeStartElement("person");
            writer.writeAttribute("id", p.id);

            // firstname / lastname
            if (p.firstName != null) {
                writer.writeStartElement("firstname");
                writer.writeCharacters(p.firstName);
                writer.writeEndElement();
            }
            if (p.surname != null) {
                writer.writeStartElement("lastname");
                writer.writeCharacters(p.surname);
                writer.writeEndElement();
            }

            // gender
            if (p.gender != null) {
                writer.writeStartElement("gender");
                writer.writeCharacters(p.gender);
                writer.writeEndElement();
            }

            // relations
            boolean hasRelations =
                    !p.parentsId.isEmpty() || p.fatherName != null || p.motherName != null ||
                            p.spouseName != null  || p.wifeId != null || p.husbandId != null ||
                            !p.sonId.isEmpty() || !p.daughterId.isEmpty() || !p.childrenName.isEmpty() ||
                            !p.sistersName.isEmpty() || !p.brothersName.isEmpty() || !p.siblingsId.isEmpty();

            if (hasRelations) {
                writer.writeStartElement("relations");

                //есть родитель
                Set<String> writeFather = new HashSet<>();
                Set<String> writeMother = new HashSet<>();

                if (!p.parentsId.isEmpty()) {
                    for (String parentId : p.parentsId)
                    {
                        if(parentId.equals("UNKNOWN")) {
                            continue;
                        }
                        if(personsById.get(parentId) == null) {
                            throw new Exception("чето с существованием родителя");
                        }
                        String parentGender = personsById.get(parentId).gender;
                        if(parentGender == null) {
                            writer.writeStartElement("parent");
                            writer.writeAttribute("ref", parentId);
                            writer.writeEndElement();
                        }
                        if (parentGender.equals("male")) {
                            writeFather.add(parentId);
                        } else if (parentGender.equals("female")) {
                            writeMother.add(parentId);
                        } else {
                            throw new Exception("чето с полом родителя не так");
                        }

                    }
                }
                for(String fatherId : writeFather) {
                    writer.writeStartElement("father");
                    writer.writeAttribute("ref", fatherId);
                    writer.writeEndElement();
                }
                if (p.fatherName != null) { //есть имя, ищем id
                    if(personsByName.get(p.fatherName) == null) {
                        throw new Exception("чето с существованием отца");
                    }
                    String fatherId = personsByName.get(p.fatherName).id;
                    if(fatherId == null) {
                        throw new Exception("чето с отцом не так");
                    }
                    if(!writeFather.contains(fatherId)) {
                        writer.writeStartElement("father");
                        writer.writeAttribute("ref", fatherId);
                        writer.writeEndElement();
                    }
                }
                for(String motherId : writeMother) {
                    writer.writeStartElement("mother");
                    writer.writeAttribute("ref", motherId);
                    writer.writeEndElement();
                }
                if (p.motherName != null) { //есть имя, ищем id
                    if(personsByName.get(p.motherName) == null) {
                        throw new Exception("чето с существованием матери");
                    }
                    String motherId = personsByName.get(p.motherName).id;
                    if(motherId == null) {
                        throw new Exception("чето с матерью не так");
                    }
                    if(!writeMother.contains(motherId)) {
                        writer.writeStartElement("mother");
                        writer.writeAttribute("ref", motherId);
                        writer.writeEndElement();
                    }
                }

                // супруг/а
                if (p.spouseName != null) {
                    if(personsByName.get(p.spouseName) == null) {
                        throw new Exception("чето с существованием супруга/и");
                    }
                    String spouceId = personsByName.get(p.spouseName).id;
                    String spouceGender = personsByName.get(p.spouseName).gender;
                    if(spouceId == null) {
                        throw new Exception("чето с супругом не так id");

                    }
                    if (spouceGender == null) {
                        writer.writeStartElement("spouse");
                        writer.writeAttribute("ref", spouceId);
                        writer.writeEndElement();
                    } else if (spouceGender.equals("male")) {
                        writer.writeStartElement("husband");
                        writer.writeAttribute("ref", spouceId);
                        writer.writeEndElement();
                    } else if (spouceGender.equals("female")) {
                        writer.writeStartElement("wife");
                        writer.writeAttribute("ref", spouceId);
                        writer.writeEndElement();
                    } else {
                        throw new Exception("чето с полом супруга/и не так");
                    }
                } else if( p.husbandId != null) {
                    writer.writeStartElement("husband");
                    writer.writeAttribute("ref", p.husbandId);
                    writer.writeEndElement();
                } else if (p.wifeId != null) {
                    writer.writeStartElement("wife");
                    writer.writeAttribute("ref", p.wifeId);
                    writer.writeEndElement();
                }

                // дети
                // другие дети (если есть в childrenId, но не в son/daughter)
                for (String childName : p.childrenName) {
                    if(personsByName.get(childName) == null) {
                        throw new Exception("чето с существованием ребенка не так");
                    }
                    String childId = personsByName.get(childName).id;
                    String childGender = personsByName.get(childName).gender;
                    if(childId == null)  {
                        throw new Exception("чето с ребенком не так, id");

                    }
                    if(childGender == null) {
                        //аналогично с sibling вдруг нет гендера
                        writer.writeStartElement("child");
                        writer.writeAttribute("ref", childId);
                        writer.writeEndElement();
                    } else if (childGender.equals("male")) {
                        p.sonId.add(childId);
                    } else if (childGender.equals("female")) {
                        p.daughterId.add(childId);
                    } else {
                        throw new Exception("чето с полом ребенка не так");
                    }
                }
                for (String son : p.sonId) {
                    writer.writeStartElement("son");
                    writer.writeAttribute("ref", son);
                    writer.writeEndElement();
                }
                for (String daughter : p.daughterId) {
                    writer.writeStartElement("daughter");
                    writer.writeAttribute("ref", daughter);
                    writer.writeEndElement();
                }

                // братья / сёстры — объединяем id
                Set<String> writeBrothers = new HashSet<>();
                Set<String> writeSisters = new HashSet<>();
                // остальные siblingsId, которых нет в brothers/sisters
                for (String sid : p.siblingsId) {
//                    if (!writtenSiblings.contains(sid)) {
                        if (personsById.get(sid) == null) {
                            throw new Exception("чето с существованием sibling не так");
                        }
                        String siblingGender = personsById.get(sid).gender;
                        if(siblingGender == null) {
                            writer.writeStartElement("sibling"); //сам проверил, нет в файле гендера например для P403395
                            writer.writeAttribute("ref", sid);
                            writer.writeEndElement();
                        }else if (siblingGender.equals("male")) {
                            writeBrothers.add(sid);
                        } else if (siblingGender.equals("female")) {
                            writeSisters.add(sid);
                        } else {
                            throw new Exception("чето с полом sibling не так");
                        }
//                    }
                }

                for (String id : writeBrothers) {
                    writer.writeStartElement("brother");
                    writer.writeAttribute("ref", id);
                    writer.writeEndElement();
                }
                for (String brotherName : p.brothersName) {
                    if(personsByName.get(brotherName) == null) {
                        throw new Exception("чето с существованием брата не так");
                    }
                    String brotherId = personsByName.get(brotherName).id;
                    if(brotherId == null) {
                        throw new Exception("чето с существованием id брата не так");
                    }
                    if(!writeBrothers.contains(brotherId)) {
                        writer.writeStartElement("brother");
                        writer.writeAttribute("ref", brotherId);
                        writer.writeEndElement();
                    }
                }

                for (String id : writeSisters) {
                    writer.writeStartElement("sister");
                    writer.writeAttribute("ref", id);
                    writer.writeEndElement();
                }
                for (String sisterName : p.sistersName) {
                    if(personsByName.get(sisterName) == null) {
                        throw new Exception("чето с существованием сестры не так");
                    }
                    String sisterId = personsByName.get(sisterName).id;
                    if(sisterId == null) {
                        throw new Exception("чето с существованием id сестры не так");
                    }
                    if(!writeSisters.contains(sisterId)) {
                        writer.writeStartElement("sister");
                        writer.writeAttribute("ref", sisterId);
                        writer.writeEndElement();
                    }
                }

                writer.writeEndElement(); // relations
            }

            writer.writeEndElement(); // person
        }

        writer.writeEndElement(); // people
        writer.writeEndDocument();
        writer.flush();
        writer.close();

        System.out.println("XML saved to " + file.getAbsolutePath());
    }

    public static void validateXML(File xml, File xsd) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = schemaFactory.newSchema(xsd);
        factory.setSchema(schema);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.parse(xml);

        System.out.println("XML валидный XSD");
    }


}

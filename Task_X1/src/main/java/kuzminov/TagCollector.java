//package kuzminov;
//
//import javax.xml.stream.XMLInputFactory;
//import javax.xml.stream.XMLStreamConstants;
//import javax.xml.stream.XMLStreamReader;
//import java.io.File;
//import java.io.FileInputStream;
//import java.util.HashMap;
//import java.util.Map;
//
//public class TagCollector {
//
//    public static Map<String, TagInfo> collect(File file) throws Exception {
//
//        Map<String, TagInfo> table = new HashMap<>();
//
//        XMLStreamReader r = XMLInputFactory
//                .newInstance()
//                .createXMLStreamReader(new FileInputStream(file));
//
//        while (r.hasNext()) {
//            int event = r.next();
//
//            if (event == XMLStreamConstants.START_ELEMENT) {
//                String tag = r.getLocalName();
//
//                TagInfo info = table.computeIfAbsent(tag, t -> new TagInfo());
//
//                for (int i = 0; i < r.getAttributeCount(); i++) {
//                    info.attributes.add(r.getAttributeLocalName(i));
//                }
//            }
//
//            if (event == XMLStreamConstants.END_ELEMENT) {
//                String tag = r.getLocalName();
//
//                TagInfo info = table.computeIfAbsent(tag, t -> new TagInfo());
//            }
//        }
//
//        r.close();
//        return table;
//    }
//
//    public static void printResult(Map<String, TagInfo> table) {
//
//        System.out.println("=== XML TAG STRUCTURE ===");
//
//        table.forEach((tag, info) -> {
//            System.out.println("Tag: <" + tag + ">");
//            System.out.println("  ATTRS: " +
//                    (info.attributes.isEmpty()
//                            ? "none"
//                            : info.attributes));
//            System.out.println();
//        });
//    }
//}

package uz.jtscorp.xarajatlar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

public class StringResourcesParityTest {
    private static final File RES = new File("src/main/res");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[sd]|%%");

    @Test
    public void russianHasEveryStringWithMatchingPlaceholders() throws Exception {
        assertParity("values-ru");
    }

    @Test
    public void englishHasEveryStringWithMatchingPlaceholders() throws Exception {
        assertParity("values-en");
    }

    @Test
    public void russianPluralsDeclareAllRequiredQuantities() throws Exception {
        for (Map.Entry<String, Map<String, String>> plural : plurals("values-ru").entrySet()) {
            assertEquals(plural.getKey(), Set.of("one", "few", "many", "other"),
                    plural.getValue().keySet());
        }
    }

    @Test
    public void englishPluralsDeclareOneAndOther() throws Exception {
        for (Map.Entry<String, Map<String, String>> plural : plurals("values-en").entrySet()) {
            assertEquals(plural.getKey(), Set.of("one", "other"), plural.getValue().keySet());
        }
    }

    @Test
    public void defaultDataArraysHaveTheSameLengthInEveryLanguage() throws Exception {
        for (String folder : new String[]{"values", "values-ru", "values-en"}) {
            Map<String, Integer> sizes = arraySizes(folder);
            assertEquals(folder, Integer.valueOf(8), sizes.get("default_expense_categories"));
            assertEquals(folder, Integer.valueOf(5), sizes.get("default_income_categories"));
        }
    }

    private static Map<String, Integer> arraySizes(String folder) throws Exception {
        Map<String, Integer> result = new TreeMap<>();
        for (Document document : documents(folder)) {
            NodeList nodes = document.getElementsByTagName("string-array");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element array = (Element) nodes.item(i);
                result.put(array.getAttribute("name"),
                        array.getElementsByTagName("item").getLength());
            }
        }
        return result;
    }

    private static void assertParity(String folder) throws Exception {
        Map<String, String> base = strings("values");
        Map<String, String> translated = strings(folder);
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> entry : base.entrySet()) {
            String other = translated.get(entry.getKey());
            if (other == null) {
                problems.add("missing: " + entry.getKey());
            } else if (other.trim().isEmpty()) {
                problems.add("empty: " + entry.getKey());
            } else if (!placeholders(entry.getValue()).equals(placeholders(other))) {
                problems.add("placeholders differ: " + entry.getKey());
            }
        }
        Set<String> extra = new TreeSet<>(translated.keySet());
        extra.removeAll(base.keySet());
        for (String key : extra) problems.add("not in values/: " + key);

        Map<String, Map<String, String>> basePlurals = plurals("values");
        Map<String, Map<String, String>> translatedPlurals = plurals(folder);
        for (Map.Entry<String, Map<String, String>> plural : basePlurals.entrySet()) {
            Map<String, String> items = translatedPlurals.get(plural.getKey());
            if (items == null) {
                problems.add("missing plural: " + plural.getKey());
                continue;
            }
            List<String> expected = placeholders(plural.getValue().get("other"));
            for (Map.Entry<String, String> item : items.entrySet()) {
                if (!expected.equals(placeholders(item.getValue()))) {
                    problems.add("placeholders differ: " + plural.getKey() + "/" + item.getKey());
                }
            }
        }
        assertTrue(folder + ":\n" + String.join("\n", problems), problems.isEmpty());
    }

    private static List<String> placeholders(String text) {
        List<String> found = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) found.add(matcher.group());
        found.sort(null);
        return found;
    }

    private static Map<String, String> strings(String folder) throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (Document document : documents(folder)) {
            NodeList nodes = document.getElementsByTagName("string");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                if ("false".equals(element.getAttribute("translatable"))) continue;
                result.put(element.getAttribute("name"), element.getTextContent());
            }
        }
        return result;
    }

    private static Map<String, Map<String, String>> plurals(String folder) throws Exception {
        Map<String, Map<String, String>> result = new TreeMap<>();
        for (Document document : documents(folder)) {
            NodeList nodes = document.getElementsByTagName("plurals");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element plural = (Element) nodes.item(i);
                Map<String, String> items = new TreeMap<>();
                NodeList children = plural.getElementsByTagName("item");
                for (int j = 0; j < children.getLength(); j++) {
                    Element item = (Element) children.item(j);
                    items.put(item.getAttribute("quantity"), item.getTextContent());
                }
                result.put(plural.getAttribute("name"), items);
            }
        }
        return result;
    }

    private static List<Document> documents(String folder) throws Exception {
        List<Document> result = new ArrayList<>();
        File[] files = new File(RES, folder).listFiles((dir, name) -> name.endsWith(".xml"));
        assertTrue(folder + " not found", files != null && files.length > 0);
        for (File file : files) {
            result.add(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file));
        }
        return result;
    }
}

import javax.net.ssl.HttpsURLConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JapanDict {

    public final String BASE_URL = "https://www.japandict.com/";

    private static final String POLITE_SECTION = "Keigo (polite)";
    private static final String PLAIN_SECTION = "Plain";

    private static final Pattern DECLENSION_TABLE_PATTERN = Pattern.compile(
            "<th colspan=\"2\"[^>]*>\\s*(Plain|Keigo \\(polite\\))\\s*</th>\\s*</tr>\\s*</thead>\\s*<tbody>(.*?)</tbody>",
            Pattern.DOTALL);
    private static final Pattern ROW_PATTERN = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL);
    private static final Pattern ROW_LABEL_PATTERN = Pattern.compile("<th[^>]*>\\s*(.*?)\\s*</th>", Pattern.DOTALL);
    private static final Pattern VALUE_SPAN_PATTERN = Pattern.compile(
            "<span class=\"(?:ps-3 text-nowrap|small ps-3 text-nowrap|xsmall text-muted ps-3 text-nowrap)\">(.*?)</span>",
            Pattern.DOTALL);
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&#(x?[0-9A-Fa-f]+);|&([A-Za-z]+);");

    // Fetches the HTML content for the JapanDict dictionary page.
    public String getURLString(String word) throws Exception {
        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        return getContentFromUrl(BASE_URL + encodedWord);
    }

    private String getContentFromUrl(String requestUrl) throws Exception {
        URL url = new URL(requestUrl);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "text/html");
        con.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

    public String getVerbValue(String htmlContent, String tenseLabel, int valueIndex) {
        return getDeclensionValue(htmlContent, POLITE_SECTION, tenseLabel, valueIndex);
    }

    public String getPlainValue(String htmlContent, String tenseLabel, int valueIndex) {
        return getDeclensionValue(htmlContent, PLAIN_SECTION, tenseLabel, valueIndex);
    }

    // valueIndex: 0 = written form, 1 = kana, 2 = romaji
    public String getDeclensionValue(String htmlContent, String sectionLabel, String tenseLabel, int valueIndex) {
        List<String> values = getTenseValues(htmlContent, sectionLabel, tenseLabel);
        if (valueIndex < 0 || valueIndex >= values.size()) {
            return "Not found";
        }
        return values.get(valueIndex);
    }

    private List<String> getTenseValues(String htmlContent, String sectionLabel, String tenseLabel) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return List.of();
        }

        Matcher tableMatcher = DECLENSION_TABLE_PATTERN.matcher(htmlContent);
        while (tableMatcher.find()) {
            String currentSection = cleanHtml(tableMatcher.group(1));
            if (!sectionLabel.equals(currentSection)) {
                continue;
            }

            String tableBody = tableMatcher.group(2);
            Matcher rowMatcher = ROW_PATTERN.matcher(tableBody);
            while (rowMatcher.find()) {
                String rowHtml = rowMatcher.group(1);
                String rowLabel = getRowLabel(rowHtml);
                if (!tenseLabel.equals(rowLabel)) {
                    continue;
                }

                List<String> values = getRowValues(rowHtml);
                if (!values.isEmpty()) {
                    return values;
                }
            }
        }

        return List.of();
    }

    private String getRowLabel(String rowHtml) {
        Matcher labelMatcher = ROW_LABEL_PATTERN.matcher(rowHtml);
        if (!labelMatcher.find()) {
            return "";
        }
        return cleanHtml(labelMatcher.group(1));
    }

    private List<String> getRowValues(String rowHtml) {
        List<String> values = new ArrayList<>();
        Matcher valueMatcher = VALUE_SPAN_PATTERN.matcher(rowHtml);
        while (valueMatcher.find()) {
            values.add(cleanHtml(valueMatcher.group(1)));
        }
        return values;
    }

    private String cleanHtml(String value) {
        if (value == null) {
            return "";
        }
        String withoutTags = value.replaceAll("<.*?>", "").trim();
        return decodeHtmlEntities(withoutTags);
    }

    private String decodeHtmlEntities(String text) {
        Matcher matcher = ENTITY_PATTERN.matcher(text);
        StringBuilder decoded = new StringBuilder();
        int lastMatchEnd = 0;

        while (matcher.find()) {
            decoded.append(text, lastMatchEnd, matcher.start());
            String numericEntity = matcher.group(1);
            String namedEntity = matcher.group(2);

            if (numericEntity != null) {
                decoded.append(decodeNumericEntity(numericEntity));
            } else {
                decoded.append(decodeNamedEntity(namedEntity, matcher.group(0)));
            }
            lastMatchEnd = matcher.end();
        }

        decoded.append(text.substring(lastMatchEnd));
        return decoded.toString();
    }

    private String decodeNumericEntity(String entityValue) {
        try {
            int codePoint;
            if (entityValue.startsWith("x") || entityValue.startsWith("X")) {
                codePoint = Integer.parseInt(entityValue.substring(1), 16);
            } else {
                codePoint = Integer.parseInt(entityValue, 10);
            }
            return new String(Character.toChars(codePoint));
        } catch (IllegalArgumentException e) {
            return "&#" + entityValue + ";";
        }
    }

    private String decodeNamedEntity(String entityName, String fallback) {
        return switch (entityName) {
            case "amp" -> "&";
            case "lt" -> "<";
            case "gt" -> ">";
            case "quot" -> "\"";
            case "apos" -> "'";
            case "nbsp" -> " ";
            default -> fallback;
        };
    }

    private String normalizeSearchWord(String wordItem) {
        return wordItem.split("[/\\uFF0F]")[0].trim();
    }

    // Iterates through a list of words and prints polite adjective forms.
    public void iterateList(String wordData) {
        List<String> wordList = Arrays.asList(wordData.split("\\s*,\\s*"));
        for (String wordItem : wordList) {
            String htmlContent;
            try {
                htmlContent = getURLString(normalizeSearchWord(wordItem));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String present = getVerbValue(htmlContent, "Present", 0);
            String negativePresent = getVerbValue(htmlContent, "Negative", 0);
            String past = getVerbValue(htmlContent, "Past", 0);
            String negativePast = getVerbValue(htmlContent, "Past negative", 0);

            System.out.println(wordItem + ",\n" +
                    present + ",\n" +
                    negativePresent + ",\n" +
                    past + ",\n" +
                    negativePast + ",");
        }
    }
}

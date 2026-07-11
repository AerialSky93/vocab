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

public class JishoAppVerbHelper {

    public final String BASE_URL = "https://jisho.org/search/";

    private static final Pattern INFLECTION_MODAL_PATTERN = Pattern.compile(
            "<div id=\"inflection_modal\".*?<table class=\"inflection_table\">(.*?)</table>",
            Pattern.DOTALL);
    private static final Pattern INFLECTION_ROW_PATTERN = Pattern.compile(
            "<tr>\\s*<td><strong>(.*?)</strong></td>\\s*" +
                    "<td class=\"japanese\"[^>]*>(?:.*?)<span class=\"text\">(.*?)</span></td>\\s*" +
                    "<td class=\"japanese\"[^>]*>(?:.*?)<span class=\"text\">(.*?)</span></td>\\s*</tr>",
            Pattern.DOTALL);
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&#(x?[0-9A-Fa-f]+);|&([A-Za-z]+);");

    // Fetches the HTML content for the Jisho search page.
    public String getURLString(String word) throws Exception {
        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        URL url = new URL(BASE_URL + encodedWord);
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
        List<String> values = getTenseValues(htmlContent, tenseLabel);
        if (valueIndex < 0 || valueIndex >= values.size()) {
            return "Not found";
        }
        return values.get(valueIndex);
    }

    private List<String> getTenseValues(String htmlContent, String tenseLabel) {
        String modalHtml = getInflectionModal(htmlContent);
        if (modalHtml.isEmpty()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        Matcher matcher = INFLECTION_ROW_PATTERN.matcher(modalHtml);
        while (matcher.find()) {
            String label = cleanHtml(matcher.group(1));
            if (!tenseLabel.equals(label)) {
                continue;
            }

            values.add(cleanHtml(matcher.group(2)));
            values.add(cleanHtml(matcher.group(3)));
            return values;
        }

        return List.of();
    }

    private String getInflectionModal(String htmlContent) {
        Matcher matcher = INFLECTION_MODAL_PATTERN.matcher(htmlContent);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
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
            case "#39" -> "'";
            default -> fallback;
        };
    }

    // Iterates through a list of words and prints the requested inflection forms.
    public void iterateList(String wordData) {
        List<String> wordList = Arrays.asList(wordData.split("\\s*,\\s*"));
        for (String wordItem : wordList) {
            String htmlContent;
            try {
                htmlContent = getURLString(wordItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String polite = getVerbValue(htmlContent, "Non-past, polite", 0);
            String politeNegative = getVerbValue(htmlContent, "Non-past, polite", 1);
            String past = getVerbValue(htmlContent, "Past", 0);
            String pastNegative = getVerbValue(htmlContent, "Past", 1);

            System.out.println(wordItem + ",\n" +
                    polite + ",\n" +
                    politeNegative + ",\n" +
                    past + ",\n" +
                    pastNegative + ",");
        }
    }
}

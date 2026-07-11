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

public class JLearnAppVerbHelper {

    public final String BASE_URL = "https://jlearn.net/dictionary/";

    private static final Pattern CONJUGATION_PATTERN = Pattern.compile(
            "<div class=\"jpn text125\">(.*?)</div>",
            Pattern.DOTALL);
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&#(x?[0-9A-Fa-f]+);|&([A-Za-z]+);");

    // Fetches the HTML content for the JLearn dictionary page.
    public String getURLString(String word) throws Exception {
        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        String requestUrl = BASE_URL + encodedWord;
        System.out.println("Reading from: " + requestUrl + " (verb: " + word + ")");
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

    // Extracts all visible conjugation values in a tense row.
    private List<String> getTenseValues(String htmlContent, String tenseLabel) {
        int tenseIndex = htmlContent.indexOf("<b>" + tenseLabel + "</b>");
        if (tenseIndex == -1) {
            return List.of();
        }

        int nextRowIndex = htmlContent.indexOf("<div class=\"row\">", tenseIndex + 1);
        String tenseBlock = nextRowIndex == -1
                ? htmlContent.substring(tenseIndex)
                : htmlContent.substring(tenseIndex, nextRowIndex);

        List<String> values = new ArrayList<>();
        Matcher matcher = CONJUGATION_PATTERN.matcher(tenseBlock);
        while (matcher.find()) {
            values.add(cleanHtml(matcher.group(1)));
        }
        return values;
    }

    public String getVerbValue(String htmlContent, String tenseLabel, int valueIndex) {
        List<String> values = getTenseValues(htmlContent, tenseLabel);
        if (valueIndex < 0 || valueIndex >= values.size()) {
            return "Not found";
        }
        return values.get(valueIndex);
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

    // Iterates through a list of words and prints the requested conjugation forms.
    public void iterateList(String wordData) {
        List<String> wordList = Arrays.asList(wordData.split("\\s*,\\s*"));
        for (String wordItem : wordList) {
            String htmlContent;
            try {
                htmlContent = getURLString(wordItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String polite = getVerbValue(htmlContent, "Present Indicative", 1);
            // JLearn lists two negative-polite alternatives in the same cell.
            // Use the second one (e.g. 楽しくないです) instead of 楽しくありません.
            String negativePolite = getVerbValue(htmlContent, "Present Indicative", 4);
            String past = getVerbValue(htmlContent, "Past Indicative", 0);
            String negativePast = getVerbValue(htmlContent, "Past Indicative", 2);

            System.out.println(wordItem + ",\n" +
                    polite + ",\n" +
                    negativePolite + ",\n" +
                    past + ",\n" +
                    negativePast + ",");
        }
    }
}

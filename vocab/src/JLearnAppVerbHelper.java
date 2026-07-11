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

    // Fetches the HTML content for the JLearn dictionary page.
    public String getURLString(String word) throws Exception {
        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        URL url = new URL(BASE_URL + encodedWord);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "text/html");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
        Matcher matcher = Pattern.compile("<div class=\"jpn text125\">(.*?)</div>", Pattern.DOTALL).matcher(tenseBlock);
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
        return value.replaceAll("<.*?>", "").trim();
    }

    // Iterates through a list of words and prints present/past forms.
    public void iterateList(String wordData) {
        List<String> wordList = Arrays.asList(wordData.split("\\s*,\\s*"));
        for (String wordItem : wordList) {
            String htmlContent;
            try {
                htmlContent = getURLString(wordItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String presentPlain = getVerbValue(htmlContent, "Present Indicative", 0);
            String presentPolite = getVerbValue(htmlContent, "Present Indicative", 1);
            String pastPlain = getVerbValue(htmlContent, "Past Indicative", 0);
            String pastPolite = getVerbValue(htmlContent, "Past Indicative", 1);

            System.out.println(wordItem + ",\n" +
                    presentPlain + ",\n" +
                    presentPolite + ",\n" +
                    pastPlain + ",\n" +
                    pastPolite + ",");
        }
    }
}

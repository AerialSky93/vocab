import javax.net.ssl.HttpsURLConnection;

import java.net.URL;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class JapaneseAppVerbHelper {

    public final String BASE_URL = "https://conjugator.reverso.net/conjugation-japanese-verb-";

    // Fetches the HTML content of the given verb's conjugation page
    public String getURLString(String verb) throws Exception {
        String encodedVerb = URLEncoder.encode(verb, "UTF-8");
        URL url = new URL(BASE_URL + encodedVerb + ".html");
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

    // Extracts the verb conjugation based on tense and politeness level using indexOf
    public String getVerbValue(String htmlContent, String verbType, String level) {
        int verbTypeIndex = htmlContent.indexOf(verbType);
        if (verbTypeIndex == -1) return "Not found";

        int levelIndex = htmlContent.indexOf(level, verbTypeIndex);
        if (levelIndex == -1) return "Not found";

        int firstClosingSpan = htmlContent.indexOf("value=", levelIndex) + 7;
        int firstEnd = htmlContent.indexOf(">", firstClosingSpan + 1) - 1;

        return htmlContent.substring(firstClosingSpan, firstEnd).trim();
    }

    // Iterates through a list of verbs and retrieves conjugations
    public void iterateList(String verbData) {
        List<String> verbList = Arrays.asList(verbData.split(",\s*"));
        for (String verbItem : verbList) {
            String htmlContent = null;
            String modifiedVerbItem = verbItem.replace(" ", "+");
            try {
                htmlContent = getURLString(modifiedVerbItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String presentPolite = getVerbValue(htmlContent, "Present", "Present Positive Formal");
            String pastPlain = getVerbValue(htmlContent, "Past", "Past Positive Formal");

            System.out.println(verbItem + ",\n" +
                    presentPolite + ",\n" +
                    pastPlain + ",");
        }
    }
}

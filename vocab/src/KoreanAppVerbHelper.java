import javax.net.ssl.HttpsURLConnection;

import java.net.URL;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.List;

public class KoreanAppVerbHelper {


    public final String BASE_URL = "https://koreanverb.app/?search=";

    // Fetches the HTML content of the given verb's conjugation page
    public String getURLString(String verb) throws Exception {
        URL url = new URL(BASE_URL + verb);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "text/html");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

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
        // Locate the starting index of the verbType and level
        int verbTypeIndex = htmlContent.indexOf(verbType);
        if (verbTypeIndex == -1) return "Not found";

        int levelIndex = htmlContent.indexOf(level, verbTypeIndex);
        if (levelIndex == -1) return "Not found";

        // Find the position of the conjugation value
        int firstClosingTd = htmlContent.indexOf("</td>", levelIndex) + 5;
        int secondOpeningTd = htmlContent.indexOf("<td", firstClosingTd) + 3;
        int secondClosingTd = htmlContent.indexOf(">", secondOpeningTd) + 1;
        int endingQuote = htmlContent.indexOf("<", secondClosingTd);

        return htmlContent.substring(secondClosingTd, endingQuote).trim();
    }

    // Iterates through a list of verbs and retrieves conjugations
    public void iterateList(String verbData)  {
        List<String> verbList = Arrays.asList(verbData.split("\\s*,\\s*"));
        for (String verbItem : verbList) {
            String htmlContent = null;

            // Find the last space (assuming the verb is the last word), If a space is found, replace it with '+'
            String modifiedVerbItem = verbItem;
            int lastSpaceIndex = verbItem.lastIndexOf(" ");
            if (lastSpaceIndex != -1) {
                modifiedVerbItem =  verbItem.substring(0, lastSpaceIndex) + "+" + verbItem.substring(lastSpaceIndex + 1);
            }

            try {
                htmlContent = getURLString(modifiedVerbItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Extracting different verb forms
            String presentInformalHigh = getVerbValue(htmlContent, "declarative present", "informal high");
            String presentInformalLow = getVerbValue(htmlContent, "declarative present", "informal low");
            String pastInformalHigh = getVerbValue(htmlContent, "declarative past", "informal high");
            String pastInformalLow = getVerbValue(htmlContent, "declarative past", "informal low");
            String futureInformalHigh = getVerbValue(htmlContent, "declarative future", "informal high");

            // Print results
            System.out.println(verbItem + ",\n" +
                    presentInformalHigh + ",\n" +
                    presentInformalLow + ",\n" +
                    pastInformalHigh + ",\n" +
                    pastInformalLow + ",\n" +
                    futureInformalHigh + ",");
        }
    }
}
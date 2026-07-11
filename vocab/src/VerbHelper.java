import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class VerbHelper {

    public final String VERB_URL = "https://api.verbix.com/conjugator/iv1/6153a464-b4f0-11ed-9ece-ee3761609078/1/8442/8442/";

    public String getURLString(String verb) throws IOException {
        URL url = new URL("https://api.verbix.com/conjugator/iv1/6153a464-b4f0-11ed-9ece-ee3761609078/1/8442/8442/"+verb);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        String htmlContent = content.toString();
        in.close();
        return htmlContent;
    }
    public String getVerbValue(String htmlContent, String verbType, String level) {
        int verbTypeIndex = htmlContent.indexOf(verbType);
        int levelIndex = htmlContent.indexOf(level, verbTypeIndex);
        int firstClosingSpan = htmlContent.indexOf("</span", levelIndex)+ 5;
        int secondOpeningSpan = htmlContent.indexOf("<span", firstClosingSpan)+ 5;
        int secondClosingSpan = htmlContent.indexOf(">", secondOpeningSpan)+1;
        int endingQuote = htmlContent.indexOf("<", secondClosingSpan);
        String finalValue = htmlContent.substring(secondClosingSpan,endingQuote);
        return finalValue;
    }

    public void iterateList(String verbData) throws IOException {
        List<String> verbList = Arrays.asList(verbData.split("\\s*,\\s*"));
        String commaType = ",\n";
        for (String verbItem : verbList) {
            String htmlContent = getURLString(verbItem);
            String test1 = getVerbValue(htmlContent, "Declarative Present", "informal high") + commaType;
            String test2 = getVerbValue(htmlContent, "Declarative Present", "informal low") + commaType;
            String test3 = getVerbValue(htmlContent, "Declarative Past", "informal high") + commaType;
            String test4 = getVerbValue(htmlContent, "Declarative Past", "informal low") + commaType;
            String test5 = getVerbValue(htmlContent, "Declarative Future", "informal high") + ",";
            //String test6 = verbHelper.getVerbValue(htmlContent, "Declarative Present", " formal high") + ",";
            System.out.println(verbItem + ",\n" + test1 + test2 + test3 + test4 + test5);
        }
    }
}
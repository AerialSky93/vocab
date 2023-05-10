import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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


}
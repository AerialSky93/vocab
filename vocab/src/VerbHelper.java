import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class VerbHelper {

    public final String VERB_URL = "https://api.verbix.com/conjugator/iv1/6153a464-b4f0-11ed-9ece-ee3761609078/1/8442/8442/";

    public String getURLString(String verb) throws IOException {
        URL url = toVerbUrl(verb);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int status = con.getResponseCode();
        if (status != 200) {
            throw new IOException("Server returned HTTP response code: " + status + " for URL: " + url);
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

    private URL toVerbUrl(String verb) throws IOException {
        try {
            URI uri = URI.create(VERB_URL).resolve(new URI(null, null, verb, null));
            return URI.create(uri.toASCIIString()).toURL();
        } catch (URISyntaxException e) {
            throw new IOException("Could not build conjugator URL for: " + verb, e);
        }
    }

    public String getVerbValue(String htmlContent, String verbType, String level) {
        int verbTypeIndex = htmlContent.indexOf(verbType);
        if (verbTypeIndex == -1) {
            return "Not found";
        }
        int levelIndex = htmlContent.indexOf(level, verbTypeIndex);
        if (levelIndex == -1) {
            return "Not found";
        }
        int firstClosingSpan = htmlContent.indexOf("</span", levelIndex) + 5;
        int secondOpeningSpan = htmlContent.indexOf("<span", firstClosingSpan) + 5;
        int secondClosingSpan = htmlContent.indexOf(">", secondOpeningSpan) + 1;
        int endingQuote = htmlContent.indexOf("<", secondClosingSpan);
        if (secondClosingSpan <= 0 || endingQuote <= secondClosingSpan) {
            return "Not found";
        }
        return htmlContent.substring(secondClosingSpan, endingQuote);
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
            System.out.println(verbItem + ",\n" + test1 + test2 + test3 + test4 + test5);
        }
    }
}

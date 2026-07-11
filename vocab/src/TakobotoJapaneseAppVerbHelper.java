import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TakobotoJapaneseAppVerbHelper {

    public final String BASE_URL = "https://takoboto.jp/?q=";
    public final String DETAIL_URL = "https://takoboto.jp/?w=";

    private static final Pattern CONJUGATION_VALUE_PATTERN = Pattern.compile(
            "<span style=\"font-size:19px\">\\s*(.*?)\\s*<span style=\"color:#A0A0A0\">",
            Pattern.DOTALL);
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&#(x?[0-9A-Fa-f]+);|&([A-Za-z]+);");
    private static final Pattern RESULT_WORD_ID_PATTERN = Pattern.compile("ResultWordId0\" value=\"(\\d+)\"");
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
    };

    // Fetches the HTML content for the Takoboto search page.
    public String getURLString(String word) throws Exception {
        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        return getContentFromUrl(BASE_URL + encodedWord);
    }

    private String getDetailURLString(String wordId) throws Exception {
        return getContentFromUrl(DETAIL_URL + wordId);
    }

    private String getContentFromUrl(String requestUrl) throws Exception {
        URL url = new URL(requestUrl);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, TRUST_ALL_CERTS, new SecureRandom());
        con.setSSLSocketFactory(sslContext.getSocketFactory());
        con.setHostnameVerifier((hostname, session) -> true);
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

    private String getDetailHtml(String word) throws Exception {
        String searchHtml = getURLString(word);
        Matcher matcher = RESULT_WORD_ID_PATTERN.matcher(searchHtml);
        if (!matcher.find()) {
            return searchHtml;
        }
        return getDetailURLString(matcher.group(1));
    }

    // Extracts conjugation values from a Takoboto conjugation card.
    private List<String> getTenseValues(String htmlContent, String tenseLabel) {
        String conjugatedFormsSection = getConjugatedFormsSection(htmlContent);
        int labelIndex = conjugatedFormsSection.indexOf(tenseLabel);
        if (labelIndex == -1) {
            return List.of();
        }

        int nextCardIndex = findNextCardIndex(conjugatedFormsSection, labelIndex + tenseLabel.length());
        String tenseBlock = nextCardIndex == -1
                ? conjugatedFormsSection.substring(labelIndex)
                : conjugatedFormsSection.substring(labelIndex, nextCardIndex);

        List<String> values = new ArrayList<>();
        Matcher matcher = CONJUGATION_VALUE_PATTERN.matcher(tenseBlock);
        while (matcher.find()) {
            values.add(cleanHtml(matcher.group(1)));
        }
        return values;
    }

    private int findNextCardIndex(String conjugatedFormsSection, int searchStartIndex) {
        int nextVisibleCardIndex = conjugatedFormsSection.indexOf(
                "<span style=\"display:inline-block;vertical-align:top",
                searchStartIndex);
        int nextHiddenCardIndex = conjugatedFormsSection.indexOf(
                "<span style=\"display:none;vertical-align:top",
                searchStartIndex);

        if (nextVisibleCardIndex == -1) {
            return nextHiddenCardIndex;
        }
        if (nextHiddenCardIndex == -1) {
            return nextVisibleCardIndex;
        }
        return Math.min(nextVisibleCardIndex, nextHiddenCardIndex);
    }

    private String getConjugatedFormsSection(String htmlContent) {
        int startIndex = htmlContent.indexOf("Conjugated forms");
        if (startIndex == -1) {
            return htmlContent;
        }

        int endIndex = htmlContent.indexOf("Kanjis", startIndex);
        if (endIndex == -1) {
            return htmlContent.substring(startIndex);
        }

        return htmlContent.substring(startIndex, endIndex);
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

    private String normalizeSearchWord(String wordItem) {
        return wordItem.split("[/\\uFF0F]")[0].trim();
    }

    // Iterates through a list of words and prints the same four forms used by
    // JLearn.
    public void iterateList(String wordData) {
        List<String> wordList = Arrays.asList(wordData.split("\\s*,\\s*"));
        for (String wordItem : wordList) {
            String htmlContent;
            try {
                htmlContent = getDetailHtml(normalizeSearchWord(wordItem));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String present = getVerbValue(htmlContent, "Present, Future", 0);
            String negativePresent = getVerbValue(htmlContent, "Present, Future", 1);
            String past = getVerbValue(htmlContent, "Past", 0);
            String negativePast = getVerbValue(htmlContent, "Past", 1);

            System.out.println(wordItem + ",\n" +
                    present + ",\n" +
                    negativePresent + ",\n" +
                    past + ",\n" +
                    negativePast + ",");
        }
    }
}

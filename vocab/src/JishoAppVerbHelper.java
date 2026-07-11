import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class JishoAppVerbHelper {

    public final String BASE_URL = "https://jisho.org/search/";

    private static final Pattern INFLECTION_ROW_PATTERN = Pattern.compile(
            "<tr>\\s*<td><strong>(.*?)</strong></td>\\s*" +
                    "<td class=\"japanese\"[^>]*>(?:.*?)<span class=\"text\">(.*?)</span></td>\\s*" +
                    "<td class=\"japanese\"[^>]*>(?:.*?)<span class=\"text\">(.*?)</span></td>\\s*</tr>",
            Pattern.DOTALL);
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&#(x?[0-9A-Fa-f]+);|&([A-Za-z]+);");

    private WebDriver driver;

    // Fetches the rendered inflection table after clicking "Show inflections".
    public String getURLString(String word) throws Exception {
        String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
        WebDriver activeDriver = getDriver();
        activeDriver.get(BASE_URL + encodedWord);

        WebDriverWait wait = new WebDriverWait(activeDriver, Duration.ofSeconds(20));
        WebElement showInflectionsLink = wait.until(
                ExpectedConditions.elementToBeClickable(By.cssSelector("a.show_inflection_table")));

        try {
            showInflectionsLink.click();
        } catch (RuntimeException e) {
            ((JavascriptExecutor) activeDriver).executeScript("arguments[0].click();", showInflectionsLink);
        }

        WebElement inflectionTable = wait.until(
                ExpectedConditions
                        .visibilityOfElementLocated(By.cssSelector("#inflection_modal table.inflection_table")));
        return inflectionTable.getAttribute("outerHTML");
    }

    public String getVerbValue(String htmlContent, String tenseLabel, int valueIndex) {
        List<String> values = getTenseValues(htmlContent, tenseLabel);
        if (values.isEmpty() && "Non-past, polite".equals(tenseLabel)) {
            values = getTenseValues(htmlContent, "Non-past");
        }
        if (valueIndex < 0 || valueIndex >= values.size()) {
            return "Not found";
        }
        return values.get(valueIndex);
    }

    private List<String> getTenseValues(String htmlContent, String tenseLabel) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        Matcher matcher = INFLECTION_ROW_PATTERN.matcher(htmlContent);
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

    private WebDriver getDriver() {
        if (driver == null) {
            driver = createDriver();
        }
        return driver;
    }

    private WebDriver createDriver() {
        RuntimeException edgeFailure = null;

        try {
            EdgeOptions options = new EdgeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=1400,1200");
            return new EdgeDriver(options);
        } catch (RuntimeException e) {
            edgeFailure = e;
        }

        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=1400,1200");
            return new ChromeDriver(options);
        } catch (RuntimeException e) {
            if (edgeFailure != null) {
                e.addSuppressed(edgeFailure);
            }
            throw e;
        }
    }

    private void closeDriver() {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } finally {
            driver = null;
        }
    }

    private String normalizeSearchWord(String wordItem) {
        return wordItem.split("[/\\uFF0F]")[0].trim();
    }

    // Iterates through a list of words and prints the requested inflection forms.
    public void iterateList(String wordData) {
        List<String> wordList = Arrays.asList(wordData.split("\\s*,\\s*"));
        try {
            for (String wordItem : wordList) {
                String htmlContent;
                try {
                    htmlContent = getURLString(normalizeSearchWord(wordItem));
                } catch (TimeoutException e) {
                    htmlContent = "";
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
        } finally {
            closeDriver();
        }
    }
}

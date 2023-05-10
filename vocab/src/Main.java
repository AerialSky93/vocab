import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        String verbData = "사다,아프다,피곤하다,결혼하다,빨래하다,요리하다,청소하다,졸업하다";
        VerbHelper verbHelper = new VerbHelper();
        List<String> verbList = Arrays.asList(verbData.split("\\s*,\\s*"));
        for (String verbItem : verbList) {
            String htmlContent = verbHelper.getURLString(verbItem);
            String test1 = verbHelper.getVerbValue(htmlContent, "Declarative Present", "informal high") + ",";
            String test2 = verbHelper.getVerbValue(htmlContent, "Declarative Present", "informal low") + ",";
            String test3 = verbHelper.getVerbValue(htmlContent, "Declarative Past", "informal high") + ",";
            String test4 = verbHelper.getVerbValue(htmlContent, "Declarative Past", "informal low") + ",";
            String test5 = verbHelper.getVerbValue(htmlContent, "Declarative Future", "informal high") + ",";
            String test6 = verbHelper.getVerbValue(htmlContent, "Declarative Present", " formal high") + ",";
            System.out.println(verbItem + ", \n" + test1 + "\n" + test2 + "\n" + test3+ "\n" + test4+ "\n" + test5+ "\n" + test6);
        }

    }
}
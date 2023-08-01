import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        String verbData = "조용하다,시끄럽다,다르다,출발하다,들다,지각하다,밝다";
        VerbHelper verbHelper = new VerbHelper();
        List<String> verbList = Arrays.asList(verbData.split("\\s*,\\s*"));
        String commaType = ",\n";
        for (String verbItem : verbList) {
            String htmlContent = verbHelper.getURLString(verbItem);
            String test1 = verbHelper.getVerbValue(htmlContent, "Declarative Present", "informal high") + commaType;
            String test2 = verbHelper.getVerbValue(htmlContent, "Declarative Present", "informal low") + commaType;
            String test3 = verbHelper.getVerbValue(htmlContent, "Declarative Past", "informal high") + commaType;
            String test4 = verbHelper.getVerbValue(htmlContent, "Declarative Past", "informal low") + commaType;
            String test5 = verbHelper.getVerbValue(htmlContent, "Declarative Future", "informal high") + ",";
            //String test6 = verbHelper.getVerbValue(htmlContent, "Declarative Present", " formal high") + ",";
            System.out.println(verbItem + ",\n" + test1 + test2 + test3 + test4 + test5);
        }

    }
}
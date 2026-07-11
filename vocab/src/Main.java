import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        String verbData = "たのしい";
        JLearnAppVerbHelper verbHelper = new JLearnAppVerbHelper();
        verbHelper.iterateList(verbData);
    }
}
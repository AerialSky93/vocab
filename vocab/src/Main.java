import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String verbData = "にぎやか,元気,大変,まあまあ,有名,好き,大好き";
        JLearnAppVerbHelper verbHelper = new JLearnAppVerbHelper();
        verbHelper.iterateList(verbData);
    }
}
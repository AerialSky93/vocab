import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String verbData = "大変";
        JapanDict verbHelper = new JapanDict();
        verbHelper.iterateList(verbData);
    }
}
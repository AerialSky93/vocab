import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String verbData = "歌う";
        TakobotoJapaneseAppVerbHelper verbHelper = new TakobotoJapaneseAppVerbHelper();
        verbHelper.iterateList(verbData);
    }
}
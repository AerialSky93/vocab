import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String verbData = "전공하다,결심하다,없어지다,즐겁다,정하다";
        VerbHelper verbHelper = new VerbHelper();
        verbHelper.iterateList(verbData);
    }
}

//        VerbHelper verbHelper = new VerbHelper();
//        JapaneseAppVerbHelper verbHelper = new JapaneseAppVerbHelper();
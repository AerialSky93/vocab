import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String verbData = "참다,굴러가다,괴롭히다,평탄하다,거절하다,넘어가다";
        VerbHelper verbHelper = new VerbHelper();
        verbHelper.iterateList(verbData);
    }
}
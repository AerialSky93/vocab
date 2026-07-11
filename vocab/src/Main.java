import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        String verbData = "歌う,かかる,新しい,古い,大きい,小さい,高い,安い,暑い／熱い,寒い,おいしい,いい,かっこいい,かわいい,面白い,楽しい,つまらない,忙しい,易しい／優しい,難しい,綺麗,静か,にぎやか,元気,大変,まあまあ,有名,好き,大好き";
        JLearnAppVerbHelper verbHelper = new JLearnAppVerbHelper();
        verbHelper.iterateList(verbData);
    }
}
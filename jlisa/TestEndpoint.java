import it.unive.jlisa.Main;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestEndpoint {
    public static void main(String[] args) throws Exception {
        String outDir = "tests-output/endpoint-test";
        Files.createDirectories(Paths.get(outDir));
        
        String[] mainArgs = {
            "-s", "examples/user-service/src/main/java/com/example/userservice/controller",
            "-o", outDir,
            "-n", "ConstantPropagation",
            "-l", "INFO"
        };
        
        Main.main(mainArgs);
        System.out.println("Analysis complete. Check endpoint info in CFG descriptors.");
    }
}

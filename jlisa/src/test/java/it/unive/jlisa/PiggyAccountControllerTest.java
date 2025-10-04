
package it.unive.jlisa;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.helpers.TestHelpers;
import it.unive.lisa.LiSA;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PiggyAccountControllerTest {

	@Test
	public void runOnAccountController() throws IOException {
		JavaFrontend frontend = new JavaFrontend();
		List<String> inputs = new ArrayList<>();
		inputs.add(
				"C:/Users/delar/piggymetrics/account-service/src/main/java/com/piggymetrics/account/controller/AccountController.java");
		frontend.parseFromListOfFile(inputs);

		LiSA lisa = TestHelpers.getLiSA("tests-output/piggymetrics/account-controller");
		lisa.run(frontend.getProgram());
	}
}

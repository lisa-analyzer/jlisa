package it.unive.jlisa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.jlisa.cli.CommandExit;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Main}: the failures of the commands do not exit the virtual
 * machine when jlisa is run through {@link Main#run(String[])} (e.g., by the
 * tests), but throw a {@link CommandExit}.
 */
public class MainTest {

	@Test
	public void failuresThrowInsteadOfExiting() {
		assertEquals(1, assertThrows(CommandExit.class, () -> Main.run(new String[0])).getCode());
		assertEquals(1, assertThrows(CommandExit.class, () -> Main.run(new String[] { "unknown" })).getCode());
		// an invalid option of check
		assertEquals(1, assertThrows(CommandExit.class,
				() -> Main.run(new String[] { "check", "--time-limit", "-5", "-s", "x.java", "-o", "out" }))
				.getCode());
	}
}

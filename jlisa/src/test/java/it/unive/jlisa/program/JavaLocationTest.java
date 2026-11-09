package it.unive.jlisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.frontend.JavaFrontend;
import it.unive.jlisa.program.cfg.statement.JavaCompoundAssignment;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JavaLocationTest {

	private static List<Statement> statements;

	@BeforeAll
	static void parse()
			throws IOException {
		Program program = new JavaFrontend().parseFromListOfFile(List.of("java-testcases/locations/Main.java"));
		CFG main = program.getAllCFGs().stream()
				.filter(cfg -> cfg.getDescriptor().getName().equals("main"))
				.findFirst()
				.orElseThrow();
		statements = new ArrayList<>();
		for (Statement st : main.getNodes())
			collect(st);
	}

	private static void collect(
			Statement st) {
		statements.add(st);
		if (st instanceof NaryExpression nary)
			for (Statement sub : nary.getSubExpressions())
				collect(sub);
	}

	private static List<Statement> at(
			int line,
			int col,
			int endLine,
			int endCol) {
		return statements.stream()
				.filter(st -> st.getLocation() instanceof JavaLocation loc
						&& loc.getLine() == line && loc.getCol() == col
						&& loc.getEndLine() == endLine && loc.getEndCol() == endCol)
				.collect(Collectors.toList());
	}

	@Test
	public void testCallEndsOnClosingParenthesis() {
		List<Statement> found = at(11, 10, 12, 5);
		assertEquals(1, found.size());
		assertTrue(found.get(0) instanceof UnresolvedCall);
	}

	@Test
	public void testInfixChainSpansFromFirstOperand() {
		assertEquals(1, at(10, 10, 10, 14).size());
		assertEquals(1, at(10, 10, 10, 18).size());
	}

	@Test
	public void testCompoundAssignmentIsOneNode() {
		List<Statement> found = at(13, 2, 13, 7);
		assertEquals(1, found.size());
		assertTrue(found.get(0) instanceof JavaCompoundAssignment);
	}

	@Test
	public void testLocationsAreUnique() {
		long distinct = statements.stream().map(st -> st.getLocation().getCodeLocation()).distinct().count();
		assertEquals(statements.size(), distinct);
	}

	@Test
	public void testStringAndEquality() {
		JavaLocation loc = new JavaLocation("Main.java", 11, 10, 12, 5);
		assertEquals("'Main.java':11:10-12:5", loc.toString());
		assertEquals(loc.toString(), loc.getCodeLocation());
		assertEquals(loc, new JavaLocation("Main.java", 11, 10, 12, 5));
		assertNotEquals(loc, new JavaLocation("Main.java", 11, 10, 11, 20));
		assertFalse(loc.equals(new SourceCodeLocation("Main.java", 11, 10)));
	}

	@Test
	public void testOrderingUsesTheEnd() {
		JavaLocation shorter = new JavaLocation("Main.java", 10, 10, 10, 14);
		JavaLocation longer = new JavaLocation("Main.java", 10, 10, 10, 18);
		assertTrue(shorter.compareTo(longer) < 0);
		assertTrue(longer.compareTo(shorter) > 0);
		assertEquals(0, shorter.compareTo(new JavaLocation("Main.java", 10, 10, 10, 14)));
	}

	@Test
	public void testEndBeforeStartIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new JavaLocation("Main.java", 10, 10, 9, 0));
		assertThrows(IllegalArgumentException.class, () -> new JavaLocation("Main.java", 10, 10, 10, 9));
	}
}

package it.unive.jlisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.jlisa.analysis.value.JavaTarsis;
import it.unive.jlisa.program.operator.JavaStringContainsOperator;
import it.unive.jlisa.program.operator.JavaStringEndsWithOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithOperator;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class JavaTarsisTest {

	private final JavaTarsis tarsis = new JavaTarsis();

	private static RegexAutomaton strings(
			String... strings) {
		SortedSet<String> set = new TreeSet<>();
		for (String s : strings)
			set.add(s);
		return JavaTarsis.fromLanguage(set);
	}

	private static RegexAutomaton prefixed(
			String prefix) {
		return RegexAutomaton.string(prefix).concat(RegexAutomaton.topString());
	}

	@Test
	public void testLanguage() {
		SortedSet<String> lang = JavaTarsis.language(strings("a", "bc", ""));
		assertEquals(new TreeSet<>(java.util.List.of("", "a", "bc")), lang);
		SortedSet<String> top = JavaTarsis.language(RegexAutomaton.TOP);
		assertEquals(1, top.size());
		assertTrue(JavaTarsis.language(prefixed("ab")).contains("ab" + JavaTarsis.UNKNOWN));
		assertEquals("abc", JavaTarsis.singleString(RegexAutomaton.string("ab").concat(RegexAutomaton.string("c"))));
	}

	@Test
	public void testEquals() {
		RegexAutomaton ab = RegexAutomaton.string("ab");
		// the atoms are split differently, but the strings are the same
		RegexAutomaton aThenB = RegexAutomaton.string("a").concat(RegexAutomaton.string("b"));
		assertEquals(Satisfiability.SATISFIED,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, ab, aThenB));
		assertEquals(Satisfiability.NOT_SATISFIED,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, strings("x", "y"), ab));
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, strings("ab", "y"), ab));
		// "a" followed by any string may be "abc": the intersection of the
		// automata would be empty
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, prefixed("a"),
						RegexAutomaton.string("abc")));
		assertEquals(Satisfiability.NOT_SATISFIED,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, prefixed("b"),
						RegexAutomaton.string("abc")));
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, RegexAutomaton.TOP, ab));
	}

	@Test
	public void testStartsEndsContains() {
		RegexAutomaton s = prefixed("<a>");
		assertEquals(Satisfiability.SATISFIED,
				tarsis.satisfiesPredicate(JavaStringStartsWithOperator.INSTANCE, s, RegexAutomaton.string("<a")));
		assertEquals(Satisfiability.NOT_SATISFIED,
				tarsis.satisfiesPredicate(JavaStringStartsWithOperator.INSTANCE, s, RegexAutomaton.string("<b")));
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringStartsWithOperator.INSTANCE, s, RegexAutomaton.string("<a>x")));
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringEndsWithOperator.INSTANCE, s, RegexAutomaton.string("x")));
		assertEquals(Satisfiability.SATISFIED,
				tarsis.satisfiesPredicate(JavaStringContainsOperator.INSTANCE, s, RegexAutomaton.string("a")));
		// the searched string may straddle the known and the unknown part
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringContainsOperator.INSTANCE, s, RegexAutomaton.string("<bad/>")));
		assertEquals(Satisfiability.NOT_SATISFIED,
				tarsis.satisfiesPredicate(JavaStringContainsOperator.INSTANCE, strings("safe", "also safe"),
						RegexAutomaton.string("<bad/>")));
		assertEquals(Satisfiability.SATISFIED,
				tarsis.satisfiesPredicate(JavaStringContainsOperator.INSTANCE, s, RegexAutomaton.emptyStr()));
	}

	@Test
	public void testRefine() {
		// assuming s.equals("abc") for any s
		RegexAutomaton refined = tarsis.refine(JavaStringEqualsOperator.INSTANCE, RegexAutomaton.TOP,
				RegexAutomaton.string("abc"), true, true);
		assertEquals("abc", JavaTarsis.singleString(refined));
		// assuming s.startsWith("ab") for any s
		refined = tarsis.refine(JavaStringStartsWithOperator.INSTANCE, RegexAutomaton.TOP,
				RegexAutomaton.string("ab"), true, true);
		assertEquals(Satisfiability.SATISFIED, tarsis.satisfiesPredicate(JavaStringStartsWithOperator.INSTANCE,
				refined, RegexAutomaton.string("a")));
		// assuming !s.equals("x") for s in {x, y}
		refined = tarsis.refine(JavaStringEqualsOperator.INSTANCE, strings("x", "y"), RegexAutomaton.string("x"),
				true, false);
		assertEquals("y", JavaTarsis.singleString(refined));
		// assuming s.equals("z") for s in {x, y}
		refined = tarsis.refine(JavaStringEqualsOperator.INSTANCE, strings("x", "y"), RegexAutomaton.string("z"),
				true, true);
		assertTrue(refined.isBottom());
		// nothing is learned on any string from its negation
		assertNull(tarsis.refine(JavaStringEqualsOperator.INSTANCE, RegexAutomaton.TOP, RegexAutomaton.string("x"),
				true, false));
	}

	@Test
	public void testRefineLength() {
		// assuming s.length() >= 2 for any s
		RegexAutomaton refined = tarsis.refineLength(RegexAutomaton.TOP, new IntInterval(2, null));
		assertEquals(new IntInterval(2, null), tarsis.length(refined));
		// the other operations see any string
		assertEquals(Satisfiability.UNKNOWN,
				tarsis.satisfiesPredicate(JavaStringEqualsOperator.INSTANCE, refined, RegexAutomaton.string("ab")));
		assertNull(JavaTarsis.singleString(refined));
		assertNull(tarsis.charAt(refined, new IntInterval(0, 0)));
		// the empty string is not one of the refined strings
		assertTrue(JavaTarsis.included(refined, RegexAutomaton.TOP));
		assertTrue(!JavaTarsis.included(RegexAutomaton.emptyStr(), refined));
		assertTrue(!JavaTarsis.included(RegexAutomaton.TOP, refined));
		// assuming s.length() == 0 for any s
		assertEquals("", JavaTarsis.singleString(tarsis.refineLength(RegexAutomaton.TOP, new IntInterval(0, 0))));
		// assuming s.length() == 3 for s in {ab, abc}
		assertEquals("abc", JavaTarsis.singleString(tarsis.refineLength(strings("ab", "abc"), new IntInterval(3, 3))));
		assertTrue(tarsis.refineLength(strings("ab"), new IntInterval(3, null)).isBottom());
		// the unknown parts of a string with two may be filled in different
		// ways: such a string is kept as it is
		RegexAutomaton twoUnknowns = RegexAutomaton.topString().concat(RegexAutomaton.string("a"))
				.concat(RegexAutomaton.topString());
		RegexAutomaton longer = tarsis.refineLength(twoUnknowns, new IntInterval(3, null));
		assertTrue(JavaTarsis.included(RegexAutomaton.string("xax"), longer));
		assertTrue(JavaTarsis.included(RegexAutomaton.string("aaaa"), longer));
	}

	@Test
	public void testIntegerOperations() {
		assertEquals(new IntInterval(1, 3), tarsis.length(strings("a", "abc")));
		IntInterval prefixLength = tarsis.length(prefixed("ab"));
		assertEquals(new IntInterval(2, null), prefixLength);
		assertEquals(new IntInterval('a', 'b'), tarsis.charAt(strings("a", "bc"), new IntInterval(0, 0)));
		// the index is beyond the known prefix
		assertNull(tarsis.charAt(prefixed("ab"), new IntInterval(2, 2)));
		assertEquals(new IntInterval(1, 1), tarsis.indexOf(prefixed("abc"), RegexAutomaton.string("b")));
		assertNull(tarsis.indexOf(prefixed("abc"), RegexAutomaton.string("x")));
	}

	@Test
	public void testStringOperations() {
		RegexAutomaton sub = tarsis.substring(strings("hello", "world"), new IntInterval(1, 1), new IntInterval(3, 3));
		assertEquals(new TreeSet<>(java.util.List.of("el", "or")), JavaTarsis.language(sub));
		RegexAutomaton prefix = tarsis.substring(prefixed("hello"), new IntInterval(0, 0), new IntInterval(2, 2));
		assertEquals("he", JavaTarsis.singleString(prefix));
		assertTrue(tarsis.substring(prefixed("he"), new IntInterval(0, 0), new IntInterval(3, 3)) == tarsis.top());
		RegexAutomaton replaced = tarsis.replace(strings("a-b"), RegexAutomaton.string("-"),
				RegexAutomaton.string("+"));
		assertEquals("a+b", JavaTarsis.singleString(replaced));
		assertEquals(new TreeSet<>(java.util.List.of("1", "2")),
				JavaTarsis.language(tarsis.integers(new IntInterval(1, 2))));
	}
}

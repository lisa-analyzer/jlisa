package it.unive.jlisa.analysis.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class JavaUpperBoundsTest {

	private static Identifier var(
			int i) {
		return new Variable(JavaIntType.INSTANCE, "x" + i, SyntheticLocation.INSTANCE);
	}

	// the relations x_i < x_j for each i < j < n (if cycles is false), or
	// for each i != j (if cycles is true)
	private static ValueEnvironment<DefiniteIdSet> relations(
			int n,
			boolean cycles) {
		ValueEnvironment<DefiniteIdSet> env = new ValueEnvironment<>(
				new DefiniteIdSet(Collections.emptySet(), true));
		for (int i = 0; i < n; i++) {
			Set<Identifier> greater = new HashSet<>();
			for (int j = cycles ? 0 : i + 1; j < n; j++)
				if (i != j)
					greater.add(var(j));
			env = env.putState(var(i), new DefiniteIdSet(greater));
		}
		return env;
	}

	@Test
	public void chainsAreTheLongestOfBoundedLength() {
		ValueEnvironment<DefiniteIdSet> env = relations(6, false);
		// x0 < x1 < ... < x5
		assertEquals(5, JavaUpperBounds.longestChain(env, var(0), var(5), 8));
		assertEquals(3, JavaUpperBounds.longestChain(env, var(0), var(5), 3));
		assertEquals(2, JavaUpperBounds.longestChain(env, var(1), var(3), 8));
		assertEquals(0, JavaUpperBounds.longestChain(env, var(3), var(1), 8));
	}

	@Test
	public void chainsOnDenseRelationsAreComputedQuickly() {
		// enumerating the simple paths would take 39^7 steps
		ValueEnvironment<DefiniteIdSet> env = relations(40, true);
		assertTimeoutPreemptively(Duration.ofSeconds(10),
				() -> assertEquals(8, JavaUpperBounds.longestChain(env, var(0), var(1), 8)));
	}
}

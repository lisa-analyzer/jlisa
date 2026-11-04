package it.unive.jlisa.lattices;

import it.unive.jlisa.analysis.value.JavaTarsis;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The product of a {@link ConstantValueIntInterval} and of a
 * {@link RegexAutomaton} of the Tarsis string domain. The automaton is
 * meaningful only for strings: for any other value it is the top automaton.
 */
public class ConstantValueIntIntervalAutomaton
		implements
		BaseLattice<ConstantValueIntIntervalAutomaton> {

	public static final ConstantValueIntIntervalAutomaton TOP = new ConstantValueIntIntervalAutomaton(
			ConstantValueIntInterval.TOP, RegexAutomaton.TOP);

	public static final ConstantValueIntIntervalAutomaton BOTTOM = new ConstantValueIntIntervalAutomaton(
			ConstantValueIntInterval.BOTTOM, RegexAutomaton.BOTTOM);

	private final ConstantValueIntInterval value;

	private final RegexAutomaton automaton;

	/**
	 * The maximum number of states of an automaton.
	 */
	public static final int MAX_STATES = 32;

	/**
	 * Builds the value. The automaton is kept only if it recognizes a finite
	 * language that {@link JavaTarsis#language(RegexAutomaton)} can enumerate
	 * (and it is not too large), otherwise it is replaced by the top automaton:
	 * the operations on the other automata (in particular, the inclusion check
	 * of the fixpoint, that complements and determinizes them) are too
	 * expensive.
	 *
	 * @param value     the value of the first component
	 * @param automaton the automaton
	 */
	public ConstantValueIntIntervalAutomaton(
			ConstantValueIntInterval value,
			RegexAutomaton automaton) {
		this.value = value;
		this.automaton = automaton == RegexAutomaton.TOP || automaton.getStates().size() > MAX_STATES
				|| JavaTarsis.language(automaton) == null ? RegexAutomaton.TOP : automaton;
	}

	public ConstantValueIntInterval getValue() {
		return value;
	}

	public RegexAutomaton getAutomaton() {
		return automaton;
	}

	@Override
	public boolean isTop() {
		return value.isTop() && automaton == RegexAutomaton.TOP;
	}

	@Override
	public boolean isBottom() {
		return value.isBottom() || (automaton != RegexAutomaton.TOP && automaton.isBottom());
	}

	@Override
	public ConstantValueIntIntervalAutomaton top() {
		return TOP;
	}

	@Override
	public ConstantValueIntIntervalAutomaton bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (automaton == RegexAutomaton.TOP)
			return value.representation();
		Map<StructuredRepresentation, StructuredRepresentation> mapping = new HashMap<>();
		mapping.put(new StringRepresentation("value"), value.representation());
		mapping.put(new StringRepresentation("automaton"), automatonRepresentation());
		return new MapRepresentation(mapping);
	}

	/**
	 * Yields the representation of the automaton, listing its strings when the
	 * language is finite (converting the automaton to a regular expression may
	 * take exponential time).
	 */
	private StructuredRepresentation automatonRepresentation() {
		SortedSet<String> lang = JavaTarsis.language(automaton);
		if (lang == null)
			return automaton.representation();
		List<String> strings = new ArrayList<>();
		for (String s : lang)
			strings.add(s.isEmpty() ? "ε" : s);
		if (strings.size() == 1)
			return new StringRepresentation(strings.get(0));
		return new StringRepresentation("(" + String.join(" + ", strings) + ")");
	}

	@Override
	public ConstantValueIntIntervalAutomaton lubAux(
			ConstantValueIntIntervalAutomaton other)
			throws SemanticException {
		return new ConstantValueIntIntervalAutomaton(value.lub(other.value), lubAutomata(automaton, other.automaton));
	}

	@Override
	public ConstantValueIntIntervalAutomaton wideningAux(
			ConstantValueIntIntervalAutomaton other)
			throws SemanticException {
		// a language that keeps growing goes to top
		RegexAutomaton widened = automaton == RegexAutomaton.TOP || other.automaton == RegexAutomaton.TOP
				|| !included(other.automaton, automaton) ? RegexAutomaton.TOP : automaton;
		return new ConstantValueIntIntervalAutomaton(value.widening(other.value), widened);
	}

	@Override
	public ConstantValueIntIntervalAutomaton narrowingAux(
			ConstantValueIntIntervalAutomaton other)
			throws SemanticException {
		// the automaton is kept: narrowing it through the intersection of
		// automata would not be sound (see JavaTarsis)
		return new ConstantValueIntIntervalAutomaton(value.narrowing(other.value), automaton);
	}

	@Override
	public ConstantValueIntIntervalAutomaton glbAux(
			ConstantValueIntIntervalAutomaton other)
			throws SemanticException {
		return new ConstantValueIntIntervalAutomaton(value.glb(other.value), glbAutomata(automaton, other.automaton));
	}

	@Override
	public boolean lessOrEqualAux(
			ConstantValueIntIntervalAutomaton other)
			throws SemanticException {
		if (!value.lessOrEqual(other.value))
			return false;
		if (other.automaton == RegexAutomaton.TOP)
			return true;
		if (automaton == RegexAutomaton.TOP)
			return false;
		return included(automaton, other.automaton);
	}

	private static boolean included(
			RegexAutomaton small,
			RegexAutomaton big) {
		// both languages are finite (see the constructor)
		return JavaTarsis.included(small, big);
	}

	private static boolean hasUnknown(
			SortedSet<String> lang) {
		return lang.stream().anyMatch(s -> s.indexOf(JavaTarsis.UNKNOWN) >= 0);
	}

	private static RegexAutomaton lubAutomata(
			RegexAutomaton a,
			RegexAutomaton b)
			throws SemanticException {
		if (a == RegexAutomaton.TOP || b == RegexAutomaton.TOP)
			return RegexAutomaton.TOP;
		return a.lub(b);
	}

	/**
	 * Yields an over-approximation of the intersection of the two automata: the
	 * intersection of their languages when they are finite, one of them
	 * otherwise.
	 */
	private static RegexAutomaton glbAutomata(
			RegexAutomaton a,
			RegexAutomaton b) {
		if (a == RegexAutomaton.TOP)
			return b;
		if (b == RegexAutomaton.TOP)
			return a;
		SortedSet<String> la = JavaTarsis.language(a);
		SortedSet<String> lb = JavaTarsis.language(b);
		if (la == null || lb == null)
			return la == null ? b : a;
		if (hasUnknown(la) || hasUnknown(lb))
			return a;
		SortedSet<String> common = new TreeSet<>(la);
		common.retainAll(lb);
		return JavaTarsis.fromLanguage(common);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, automaton);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		ConstantValueIntIntervalAutomaton other = (ConstantValueIntIntervalAutomaton) obj;
		return Objects.equals(value, other.value) && Objects.equals(automaton, other.automaton);
	}
}

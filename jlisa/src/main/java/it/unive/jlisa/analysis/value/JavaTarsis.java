package it.unive.jlisa.analysis.value;

import it.unive.jlisa.program.operator.JavaStringAppendBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharArrayOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharOperator;
import it.unive.jlisa.program.operator.JavaStringAppendDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringAppendFloatOperator;
import it.unive.jlisa.program.operator.JavaStringAppendIntOperator;
import it.unive.jlisa.program.operator.JavaStringAppendLongOperator;
import it.unive.jlisa.program.operator.JavaStringAppendObjectOperator;
import it.unive.jlisa.program.operator.JavaStringAppendStringOperator;
import it.unive.jlisa.program.operator.JavaStringConcatOperator;
import it.unive.jlisa.program.operator.JavaStringContainsOperator;
import it.unive.jlisa.program.operator.JavaStringEndsWithOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.operator.JavaStringReplaceOperator;
import it.unive.jlisa.program.operator.JavaStringReverseOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithOperator;
import it.unive.jlisa.program.operator.JavaStringToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaStringToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaStringTrimOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.string.tarsis.Tarsis;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import it.unive.lisa.util.datastructures.regex.TopAtom;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The Tarsis string domain, extended with the semantics of the Java string
 * operators of {@code java.lang.String}, {@code StringBuilder} and
 * {@code StringBuffer}. Tarsis dispatches on the singleton instances of the
 * string operators of LiSA, and the Java operators are different objects, hence
 * they are modelled here.<br>
 * <br>
 * The operations that Tarsis decides through the intersection of automata are
 * not used for deciding conditions: an automaton recognizing the top string has
 * a single {@link TopAtom} transition, and the intersection treats it as a
 * single symbol (e.g., the intersection of {@code "a"} followed by top with
 * {@code "abc"} is empty). Conditions are instead decided on the finite
 * language of the automata, where each string may contain {@link #UNKNOWN},
 * standing for any (possibly empty) string. When the language is not finite or
 * too large, the result is unknown.
 */
public class JavaTarsis extends Tarsis {

	/**
	 * The character standing for any (possibly empty) string inside the strings
	 * of a language.
	 */
	public static final char UNKNOWN = TopAtom.STRING.charAt(0);

	private static final String UNKNOWN_STRING = TopAtom.STRING;

	/**
	 * The character standing for any single character inside the strings of a
	 * language. It is used to represent a string of at least a given length,
	 * e.g., any string of at least one character is the unknown character
	 * followed by {@link #UNKNOWN}. The operations of this domain (but the
	 * length) see it as {@link #UNKNOWN}, that is a sound over-approximation.
	 */
	private static final char ANY_CHAR = '\uE000';

	/**
	 * The maximum length represented with {@link #ANY_CHAR}s.
	 */
	public static final int MAX_MIN_LENGTH = 8;

	/**
	 * The maximum number of strings of a language that are enumerated.
	 */
	public static final int MAX_LANGUAGE_SIZE = 64;

	/**
	 * The maximum number of indexes (or pairs of indexes) that are enumerated
	 * when an index is an interval.
	 */
	public static final int MAX_INDEXES = 64;

	/**
	 * The maximum number of transitions visited when enumerating a language.
	 */
	private static final int MAX_VISITS = 4096;

	@Override
	public RegexAutomaton evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (constant.getValue() instanceof String s)
			return constant(s);
		return top();
	}

	@Override
	public RegexAutomaton evalUnaryExpression(
			UnaryExpression expression,
			RegexAutomaton arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		UnaryOperator operator = expression.getOperator();
		if (operator == JavaStringToLowerCaseOperator.INSTANCE)
			// the unknown marker is a letter: case is changed part by part
			return map(arg, String::toLowerCase, true, true);
		if (operator == JavaStringToUpperCaseOperator.INSTANCE)
			return map(arg, String::toUpperCase, true, true);
		if (operator == JavaStringTrimOperator.INSTANCE)
			return map(arg, String::trim, false, false);
		if (operator == JavaStringReverseOperator.INSTANCE)
			// the unknown parts are reversed into unknown parts
			return map(arg, s -> new StringBuilder(s).reverse().toString(), true, false);
		return top();
	}

	@Override
	public RegexAutomaton evalBinaryExpression(
			BinaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		if (left.isBottom() || right.isBottom())
			return bottom();
		if (operator instanceof StringConcat || operator == JavaStringConcatOperator.INSTANCE
				|| operator == JavaStringAppendStringOperator.INSTANCE)
			return left.concat(right);
		if (operator == JavaStringAppendBooleanOperator.INSTANCE
				|| operator == JavaStringAppendCharOperator.INSTANCE
				|| operator == JavaStringAppendIntOperator.INSTANCE
				|| operator == JavaStringAppendLongOperator.INSTANCE
				|| operator == JavaStringAppendFloatOperator.INSTANCE
				|| operator == JavaStringAppendDoubleOperator.INSTANCE
				|| operator == JavaStringAppendObjectOperator.INSTANCE
				|| operator == JavaStringAppendCharArrayOperator.INSTANCE)
			// the appended value is not a string: its textual representation
			// is unknown here
			return left.concat(RegexAutomaton.topString());
		return top();
	}

	@Override
	public RegexAutomaton evalTernaryExpression(
			TernaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton middle,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		TernaryOperator operator = expression.getOperator();
		if (operator == JavaStringReplaceOperator.INSTANCE)
			return replace(left, middle, right);
		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return satisfiesPredicate(expression.getOperator(), left, right);
	}

	/**
	 * Yields whether the given operator is a predicate between two strings that
	 * is decided by this domain.
	 *
	 * @param operator the operator
	 *
	 * @return {@code true} if that condition holds
	 */
	public static boolean isPredicate(
			BinaryOperator operator) {
		return predicate(operator) != null;
	}

	private static BiFunction<String, String, Satisfiability> predicate(
			BinaryOperator operator) {
		if (operator == JavaStringEqualsOperator.INSTANCE)
			return JavaTarsis::equalsString;
		if (operator == JavaStringStartsWithOperator.INSTANCE)
			return JavaTarsis::startsWithString;
		if (operator == JavaStringEndsWithOperator.INSTANCE)
			return JavaTarsis::endsWithString;
		if (operator == JavaStringContainsOperator.INSTANCE)
			return JavaTarsis::containsString;
		return null;
	}

	/**
	 * Yields whether the given predicate holds between the strings of the given
	 * automata.
	 *
	 * @param operator the predicate (see {@link #isPredicate(BinaryOperator)})
	 * @param left     the receiver of the predicate
	 * @param right    the argument of the predicate
	 *
	 * @return whether the predicate holds, or {@link Satisfiability#UNKNOWN} if
	 *             it cannot be decided
	 */
	public Satisfiability satisfiesPredicate(
			BinaryOperator operator,
			RegexAutomaton left,
			RegexAutomaton right) {
		BiFunction<String, String, Satisfiability> predicate = predicate(operator);
		if (predicate == null)
			return Satisfiability.UNKNOWN;
		SortedSet<String> l = language(left);
		SortedSet<String> r = language(right);
		if (l == null || r == null || l.isEmpty() || r.isEmpty())
			return Satisfiability.UNKNOWN;
		return combine(l, r, predicate);
	}

	/**
	 * Yields a refinement of one of the operands of the given predicate,
	 * assuming that the predicate holds (if {@code positive}) or that it does
	 * not hold.
	 *
	 * @param operator   the predicate (see
	 *                       {@link #isPredicate(BinaryOperator)})
	 * @param left       the receiver of the predicate
	 * @param right      the argument of the predicate
	 * @param refineLeft whether the left operand is refined (otherwise, the
	 *                       right one is)
	 * @param positive   whether the predicate holds
	 *
	 * @return the refined automaton, the bottom automaton if the predicate can
	 *             never have the given outcome, or {@code null} if nothing is
	 *             learned
	 */
	public RegexAutomaton refine(
			BinaryOperator operator,
			RegexAutomaton left,
			RegexAutomaton right,
			boolean refineLeft,
			boolean positive) {
		BiFunction<String, String, Satisfiability> predicate = predicate(operator);
		if (predicate == null)
			return null;
		RegexAutomaton target = refineLeft ? left : right;
		SortedSet<String> others = language(refineLeft ? right : left);
		if (others == null || others.isEmpty())
			return null;
		boolean knownOthers = others.stream().noneMatch(JavaTarsis::hasUnknown);
		SortedSet<String> targets = language(target);

		if (positive && operator == JavaStringEqualsOperator.INSTANCE && knownOthers) {
			// the target is one of the other strings
			SortedSet<String> kept = new TreeSet<>();
			for (String other : others)
				if (targets == null || targets.stream()
						.anyMatch(t -> equalsString(t, other) != Satisfiability.NOT_SATISFIED))
					kept.add(other);
			return fromLanguage(kept);
		}

		if (targets == null)
			return null;

		if (positive && refineLeft && knownOthers && targets.size() == 1 && targets.first().equals(UNKNOWN_STRING)) {
			// any string: the receiver is built around the argument
			RegexAutomaton arg = fromLanguage(others);
			if (operator == JavaStringStartsWithOperator.INSTANCE)
				return arg.concat(RegexAutomaton.topString());
			if (operator == JavaStringEndsWithOperator.INSTANCE)
				return RegexAutomaton.topString().concat(arg);
			if (operator == JavaStringContainsOperator.INSTANCE)
				return RegexAutomaton.topString().concat(arg).concat(RegexAutomaton.topString());
			return null;
		}

		SortedSet<String> kept = new TreeSet<>();
		for (String t : targets) {
			SortedSet<String> single = new TreeSet<>();
			single.add(t);
			Satisfiability sat = refineLeft ? combine(single, others, predicate) : combine(others, single, predicate);
			if (positive ? sat != Satisfiability.NOT_SATISFIED : sat != Satisfiability.SATISFIED)
				kept.add(t);
		}
		if (kept.size() == targets.size())
			return null;
		return fromLanguage(kept);
	}

	/**
	 * Yields the interval of the lengths of the strings of the given automaton.
	 *
	 * @param automaton the automaton
	 *
	 * @return the interval, or {@code null} if nothing is known
	 */
	public IntInterval length(
			RegexAutomaton automaton) {
		SortedSet<String> lang = rawLanguage(automaton);
		if (lang != null) {
			if (lang.isEmpty())
				return null;
			int min = Integer.MAX_VALUE, max = 0;
			boolean unbounded = false;
			for (String s : lang) {
				int known = knownLength(s);
				min = Math.min(min, known);
				if (hasUnknown(s))
					unbounded = true;
				else
					max = Math.max(max, known);
			}
			return new IntInterval(Integer.valueOf(min), unbounded ? null : Integer.valueOf(max));
		}
		Integer min = minLength(automaton);
		return min == null ? null : new IntInterval(min, (Integer) null);
	}

	/**
	 * Yields the interval of the characters at the given index of the strings
	 * of the given automaton. The strings that are not long enough are not
	 * considered: on them, the access raises an exception.
	 *
	 * @param automaton the automaton
	 * @param index     the index
	 *
	 * @return the interval of the codes of the characters, or {@code null} if
	 *             nothing is known
	 */
	public IntInterval charAt(
			RegexAutomaton automaton,
			IntInterval index) {
		long[] indexes = indexes(index);
		SortedSet<String> lang = language(automaton);
		if (indexes == null || lang == null)
			return null;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (String s : lang) {
			int unknown = s.indexOf(UNKNOWN);
			for (long i : indexes) {
				if (i < 0)
					continue;
				if (unknown >= 0 && i >= unknown)
					return null;
				if (i >= s.length())
					continue;
				char c = s.charAt((int) i);
				min = Math.min(min, c);
				max = Math.max(max, c);
			}
		}
		return min > max ? null : new IntInterval(min, max);
	}

	/**
	 * Yields the interval of the indexes of the first occurrence of the strings
	 * of {@code searched} inside the strings of {@code automaton}.
	 *
	 * @param automaton the automaton
	 * @param searched  the automaton of the searched strings
	 *
	 * @return the interval, or {@code null} if nothing is known
	 */
	public IntInterval indexOf(
			RegexAutomaton automaton,
			RegexAutomaton searched) {
		SortedSet<String> lang = language(automaton);
		SortedSet<String> targets = language(searched);
		if (lang == null || targets == null || lang.isEmpty() || targets.isEmpty())
			return null;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
		for (String s : lang)
			for (String t : targets) {
				if (hasUnknown(t))
					return null;
				// the first occurrence inside the known prefix is the first
				// occurrence of the whole string
				int idx = knownPrefix(s).indexOf(t);
				if (idx < 0 && hasUnknown(s))
					return null;
				min = Math.min(min, idx);
				max = Math.max(max, idx);
			}
		return new IntInterval(min, max);
	}

	/**
	 * Yields the automaton recognizing the substrings from {@code begin}
	 * (inclusive) to {@code end} (exclusive) of the strings of the given
	 * automaton. The pairs of indexes that are out of the bounds of a string
	 * are not considered: on them, the operation raises an exception.
	 *
	 * @param automaton the automaton
	 * @param begin     the begin index
	 * @param end       the end index, or {@code null} for the end of each
	 *                      string
	 *
	 * @return the automaton
	 */
	public RegexAutomaton substring(
			RegexAutomaton automaton,
			IntInterval begin,
			IntInterval end) {
		SortedSet<String> lang = language(automaton);
		long[] begins = indexes(begin);
		long[] ends = end == null ? new long[] { -1 } : indexes(end);
		if (lang == null || begins == null || ends == null || begins.length * ends.length > MAX_INDEXES)
			return top();
		SortedSet<String> result = new TreeSet<>();
		for (String s : lang) {
			int unknown = s.indexOf(UNKNOWN);
			for (long i : begins)
				for (long j : ends) {
					if (end == null) {
						if (i < 0)
							continue;
						if (unknown >= 0 && i > unknown)
							return top();
						if (i > s.length())
							continue;
						result.add(s.substring((int) i));
						continue;
					}
					if (i < 0 || i > j)
						continue;
					if (unknown >= 0 && j > unknown)
						return top();
					if (j > s.length())
						continue;
					result.add(s.substring((int) i, (int) j));
				}
		}
		if (result.isEmpty() || result.size() > MAX_LANGUAGE_SIZE)
			return top();
		return fromLanguage(result);
	}

	/**
	 * Yields the automaton recognizing the strings of {@code automaton} where
	 * each occurrence of a string of {@code target} is replaced with a string
	 * of {@code replacement}.
	 *
	 * @param automaton   the automaton
	 * @param target      the automaton of the replaced strings
	 * @param replacement the automaton of the replacements
	 *
	 * @return the automaton
	 */
	@Override
	public RegexAutomaton replace(
			RegexAutomaton automaton,
			RegexAutomaton target,
			RegexAutomaton replacement) {
		SortedSet<String> lang = known(language(automaton));
		SortedSet<String> targets = known(language(target));
		SortedSet<String> replacements = known(language(replacement));
		if (lang == null || targets == null || replacements == null
				|| lang.size() * targets.size() * replacements.size() > MAX_LANGUAGE_SIZE)
			return top();
		SortedSet<String> result = new TreeSet<>();
		for (String s : lang)
			for (String t : targets)
				for (String r : replacements)
					result.add(s.replace(t, r));
		return fromLanguage(result);
	}

	/**
	 * Yields the automaton recognizing the textual representations of the
	 * integers of the given interval.
	 *
	 * @param interval the interval
	 *
	 * @return the automaton
	 */
	public RegexAutomaton integers(
			IntInterval interval) {
		long[] values = indexes(interval);
		if (values == null)
			return top();
		SortedSet<String> result = new TreeSet<>();
		for (long v : values)
			result.add(String.valueOf(v));
		return fromLanguage(result);
	}

	/**
	 * Yields the automaton recognizing the characters whose codes are in the
	 * given interval.
	 *
	 * @param interval the interval
	 *
	 * @return the automaton
	 */
	public RegexAutomaton chars(
			IntInterval interval) {
		long[] values = indexes(interval);
		if (values == null)
			return top();
		SortedSet<String> result = new TreeSet<>();
		for (long v : values) {
			if (v < Character.MIN_VALUE || v > Character.MAX_VALUE || v == UNKNOWN || v == ANY_CHAR)
				return top();
			result.add(String.valueOf((char) v));
		}
		return fromLanguage(result);
	}

	/**
	 * Yields the automaton recognizing exactly the given string.
	 *
	 * @param s the string
	 *
	 * @return the automaton
	 */
	public RegexAutomaton constant(
			String s) {
		if (hasUnknown(s) || s.indexOf(ANY_CHAR) >= 0)
			// the character cannot be told apart from the markers
			return top();
		return fromString(s);
	}

	/**
	 * Yields the only string recognized by the given automaton.
	 *
	 * @param automaton the automaton
	 *
	 * @return the string, or {@code null} if the automaton does not recognize
	 *             exactly one known string
	 */
	public static String singleString(
			RegexAutomaton automaton) {
		SortedSet<String> lang = language(automaton);
		if (lang == null || lang.size() != 1 || hasUnknown(lang.first()))
			return null;
		return lang.first();
	}

	/**
	 * Yields the strings recognized by the given automaton, where
	 * {@link #UNKNOWN} stands for any string. The strings are
	 * over-approximated: the characters standing for any single character
	 * become unknown parts.
	 *
	 * @param automaton the automaton
	 *
	 * @return the strings, or {@code null} if the automaton recognizes
	 *             infinitely many strings, too many strings, or has transitions
	 *             that are not plain strings
	 */
	public static SortedSet<String> language(
			RegexAutomaton automaton) {
		SortedSet<String> raw = rawLanguage(automaton);
		if (raw == null)
			return null;
		SortedSet<String> result = new TreeSet<>();
		for (String s : raw)
			result.add(widen(s));
		return result;
	}

	/**
	 * Replaces the characters standing for any single character with unknown
	 * parts.
	 */
	private static String widen(
			String s) {
		if (s.indexOf(ANY_CHAR) < 0)
			return s;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i) == ANY_CHAR ? UNKNOWN : s.charAt(i);
			if (c != UNKNOWN || result.length() == 0 || result.charAt(result.length() - 1) != UNKNOWN)
				result.append(c);
		}
		return result.toString();
	}

	/**
	 * Yields the strings recognized by the given automaton, where
	 * {@link #UNKNOWN} stands for any string and {@link #ANY_CHAR} for any
	 * single character.
	 */
	private static SortedSet<String> rawLanguage(
			RegexAutomaton automaton) {
		SortedSet<String> result = new TreeSet<>();
		if (automaton == RegexAutomaton.TOP) {
			result.add(UNKNOWN_STRING);
			return result;
		}
		if (automaton.isBottom())
			return result;
		if (automaton.hasCycle())
			return null;
		int[] visits = { 0 };
		for (State q : automaton.getInitialStates())
			if (!collect(automaton, q, "", result, visits))
				return null;
		return result;
	}

	private static boolean collect(
			RegexAutomaton automaton,
			State state,
			String prefix,
			SortedSet<String> result,
			int[] visits) {
		if (state.isFinal()) {
			result.add(prefix);
			if (result.size() > MAX_LANGUAGE_SIZE)
				return false;
		}
		for (Transition<RegularExpression> t : automaton.getOutgoingTransitionsFrom(state)) {
			if (++visits[0] > MAX_VISITS)
				return false;
			RegularExpression symbol = t.getSymbol();
			String s;
			if (symbol instanceof TopAtom)
				s = prefix.endsWith(UNKNOWN_STRING) ? "" : UNKNOWN_STRING;
			else if (symbol instanceof Atom atom) {
				s = atom.getString();
				if (hasUnknown(s))
					return false;
			} else
				return false;
			if (!collect(automaton, t.getDestination(), prefix + s, result, visits))
				return false;
		}
		return true;
	}

	/**
	 * Yields whether the strings of {@code small} are surely strings of
	 * {@code big}, where {@link #UNKNOWN} stands for any string. A known string
	 * is included if it is an instance of a string of {@code big}, a string
	 * with unknown parts only if {@code big} contains it, or the unknown
	 * string: the result may be {@code false} even if the inclusion holds,
	 * never the opposite.
	 *
	 * @param small the first language
	 * @param big   the second language
	 *
	 * @return {@code true} if the inclusion surely holds
	 */
	public static boolean included(
			SortedSet<String> small,
			SortedSet<String> big) {
		if (big.contains(UNKNOWN_STRING))
			return true;
		for (String s : small)
			if (!big.contains(s) && (hasUnknown(s) || big.stream().noneMatch(b -> instanceOf(b, s))))
				return false;
		return true;
	}

	/**
	 * Yields whether the strings of {@code small} are surely strings of
	 * {@code big}: the result may be {@code false} even if the inclusion holds,
	 * never the opposite.
	 *
	 * @param small the first automaton
	 * @param big   the second automaton
	 *
	 * @return {@code true} if the inclusion surely holds
	 */
	public static boolean included(
			RegexAutomaton small,
			RegexAutomaton big) {
		if (big == RegexAutomaton.TOP || small.equals(big))
			return true;
		SortedSet<String> s = rawLanguage(small);
		SortedSet<String> b = rawLanguage(big);
		if (s == null || b == null)
			return false;
		if (b.containsAll(s))
			return true;
		// the strings of small are over-approximated, the ones of big are
		// taken as they are: the characters standing for any single
		// character are compared as plain characters, that is conservative
		SortedSet<String> widened = new TreeSet<>();
		for (String str : s)
			if (!b.contains(str))
				widened.add(widen(str));
		return included(widened, b);
	}

	/**
	 * Yields the strings of the given automaton whose length is in the given
	 * interval: the strings with an unknown part are kept, and the unknown part
	 * of a string with a single one is made long enough (up to
	 * {@link #MAX_MIN_LENGTH} characters).
	 *
	 * @param automaton the automaton
	 * @param length    the interval of the lengths
	 *
	 * @return the refined automaton, the bottom automaton if no string has a
	 *             length in the interval, or {@code null} if nothing is learned
	 */
	public RegexAutomaton refineLength(
			RegexAutomaton automaton,
			IntInterval length) {
		if (length == null || length.isBottom() || length.isTop())
			return null;
		SortedSet<String> lang = rawLanguage(automaton);
		if (lang == null || lang.isEmpty())
			return null;
		long low, high;
		try {
			low = length.lowIsMinusInfinity() ? 0 : Math.max(0, length.getLow().toLong());
			high = length.highIsPlusInfinity() ? Long.MAX_VALUE : length.getHigh().toLong();
		} catch (MathNumberConversionException e) {
			return null;
		}
		SortedSet<String> kept = new TreeSet<>();
		for (String s : lang) {
			int known = knownLength(s);
			if (!hasUnknown(s)) {
				if (known >= low && known <= high)
					kept.add(s);
				continue;
			}
			if (known > high)
				continue;
			if (known == high) {
				// the unknown parts are empty
				kept.add(s.replace(UNKNOWN_STRING, ""));
				continue;
			}
			int unknown = s.indexOf(UNKNOWN);
			if (known >= low || low > MAX_MIN_LENGTH || unknown != s.lastIndexOf(UNKNOWN)) {
				// the unknown parts of a string with more than one may be
				// filled in different ways: the string is kept as it is
				kept.add(s);
				continue;
			}
			kept.add(s.substring(0, unknown) + String.valueOf(ANY_CHAR).repeat((int) (low - known))
					+ s.substring(unknown));
		}
		if (kept.equals(lang))
			return null;
		return fromLanguage(kept);
	}

	/**
	 * Yields the automaton recognizing the given strings, where
	 * {@link #UNKNOWN} stands for any string.
	 *
	 * @param strings the strings
	 *
	 * @return the automaton
	 */
	public static RegexAutomaton fromLanguage(
			SortedSet<String> strings) {
		if (strings.isEmpty())
			return RegexAutomaton.emptyLang();
		if (strings.contains(UNKNOWN_STRING))
			return RegexAutomaton.TOP;
		RegexAutomaton result = null;
		for (String s : strings)
			result = result == null ? fromString(s) : result.union(fromString(s));
		return result;
	}

	private static RegexAutomaton fromString(
			String s) {
		RegexAutomaton result = null;
		String[] segments = s.split(Pattern.quote(UNKNOWN_STRING), -1);
		for (int i = 0; i < segments.length; i++) {
			if (!segments[i].isEmpty())
				result = append(result, RegexAutomaton.string(segments[i]));
			if (i < segments.length - 1)
				result = append(result, RegexAutomaton.topString());
		}
		return result == null ? RegexAutomaton.emptyStr() : result;
	}

	private static RegexAutomaton append(
			RegexAutomaton left,
			RegexAutomaton right) {
		return left == null ? right : left.concat(right);
	}

	/**
	 * Yields the minimum length of the strings recognized by the given
	 * automaton, where the top string has length 0.
	 */
	private static Integer minLength(
			RegexAutomaton automaton) {
		Map<State, Integer> distance = new HashMap<>();
		PriorityQueue<Map.Entry<State, Integer>> queue = new PriorityQueue<>(Map.Entry.comparingByValue());
		for (State q : automaton.getInitialStates()) {
			distance.put(q, 0);
			queue.add(Map.entry(q, 0));
		}
		while (!queue.isEmpty()) {
			Map.Entry<State, Integer> current = queue.poll();
			State q = current.getKey();
			int d = current.getValue();
			if (d > distance.get(q))
				continue;
			if (q.isFinal())
				return d;
			for (Transition<RegularExpression> t : automaton.getOutgoingTransitionsFrom(q)) {
				int next = d + Math.max(0, t.getSymbol().minLength());
				Integer old = distance.get(t.getDestination());
				if (old == null || next < old) {
					distance.put(t.getDestination(), next);
					queue.add(Map.entry(t.getDestination(), next));
				}
			}
		}
		return null;
	}

	/**
	 * Applies {@code f} to each string of the given automaton. If
	 * {@code segmentwise}, {@code f} is applied to each known part of a string
	 * on its own (the unknown parts are mapped into unknown parts), otherwise
	 * to the whole string, where it must map the unknown marker into itself.
	 * Strings with unknown parts are mapped only if {@code allowUnknown}.
	 */
	private RegexAutomaton map(
			RegexAutomaton automaton,
			Function<String, String> f,
			boolean allowUnknown,
			boolean segmentwise) {
		SortedSet<String> lang = language(automaton);
		if (lang == null || lang.isEmpty())
			return top();
		SortedSet<String> result = new TreeSet<>();
		for (String s : lang) {
			if (!isAscii(s) || (!allowUnknown && hasUnknown(s)))
				// non-ascii characters may change length or depend on the
				// surrounding characters
				return top();
			if (!hasUnknown(s) || !segmentwise) {
				result.add(f.apply(s));
				continue;
			}
			String[] segments = s.split(Pattern.quote(UNKNOWN_STRING), -1);
			StringBuilder mapped = new StringBuilder();
			for (int i = 0; i < segments.length; i++) {
				mapped.append(f.apply(segments[i]));
				if (i < segments.length - 1)
					mapped.append(UNKNOWN);
			}
			result.add(mapped.toString());
		}
		return fromLanguage(result);
	}

	private static boolean isAscii(
			String s) {
		for (int i = 0; i < s.length(); i++)
			if (s.charAt(i) > 127 && s.charAt(i) != UNKNOWN)
				return false;
		return true;
	}

	private static SortedSet<String> known(
			SortedSet<String> lang) {
		if (lang == null || lang.isEmpty() || lang.stream().anyMatch(JavaTarsis::hasUnknown))
			return null;
		return lang;
	}

	private static long[] indexes(
			IntInterval interval) {
		if (interval == null || interval.isBottom() || !interval.isFinite())
			return null;
		long low, high;
		try {
			low = interval.getLow().toLong();
			high = interval.getHigh().toLong();
		} catch (MathNumberConversionException e) {
			return null;
		}
		if (high - low + 1 > MAX_INDEXES)
			return null;
		long[] result = new long[(int) (high - low + 1)];
		for (int i = 0; i < result.length; i++)
			result[i] = low + i;
		return result;
	}

	private static Satisfiability combine(
			SortedSet<String> left,
			SortedSet<String> right,
			BiFunction<String, String, Satisfiability> predicate) {
		Satisfiability result = null;
		for (String l : left)
			for (String r : right) {
				Satisfiability sat = predicate.apply(l, r);
				if (result == null)
					result = sat;
				else if (result != sat)
					return Satisfiability.UNKNOWN;
				if (result == Satisfiability.UNKNOWN)
					return result;
			}
		return result == null ? Satisfiability.UNKNOWN : result;
	}

	private static boolean hasUnknown(
			String s) {
		return s.indexOf(UNKNOWN) >= 0;
	}

	private static int knownLength(
			String s) {
		int length = 0;
		for (int i = 0; i < s.length(); i++)
			if (s.charAt(i) != UNKNOWN)
				length++;
		return length;
	}

	private static String knownPrefix(
			String s) {
		int i = s.indexOf(UNKNOWN);
		return i < 0 ? s : s.substring(0, i);
	}

	private static String knownSuffix(
			String s) {
		int i = s.lastIndexOf(UNKNOWN);
		return i < 0 ? s : s.substring(i + 1);
	}

	private static boolean prefixCompatible(
			String a,
			String b) {
		return a.startsWith(b) || b.startsWith(a);
	}

	private static boolean suffixCompatible(
			String a,
			String b) {
		return a.endsWith(b) || b.endsWith(a);
	}

	/**
	 * Yields whether the known string {@code s} is an instance of
	 * {@code pattern}, where {@link #UNKNOWN} stands for any string.
	 */
	private static boolean instanceOf(
			String pattern,
			String s) {
		boolean[] current = new boolean[s.length() + 1];
		current[0] = true;
		for (int p = 0; p < pattern.length(); p++) {
			boolean[] next = new boolean[s.length() + 1];
			char c = pattern.charAt(p);
			for (int i = 0; i <= s.length(); i++)
				if (c == UNKNOWN)
					next[i] = current[i] || (i > 0 && next[i - 1]);
				else
					next[i] = i > 0 && current[i - 1] && s.charAt(i - 1) == c;
			current = next;
		}
		return current[s.length()];
	}

	private static Satisfiability equalsString(
			String a,
			String b) {
		boolean ua = hasUnknown(a), ub = hasUnknown(b);
		if (!ua && !ub)
			return Satisfiability.fromBoolean(a.equals(b));
		if (!ua)
			return instanceOf(b, a) ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		if (!ub)
			return instanceOf(a, b) ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		// equal strings start with both known prefixes and end with both
		// known suffixes
		return prefixCompatible(knownPrefix(a), knownPrefix(b)) && suffixCompatible(knownSuffix(a), knownSuffix(b))
				? Satisfiability.UNKNOWN
				: Satisfiability.NOT_SATISFIED;
	}

	private static Satisfiability startsWithString(
			String s,
			String prefix) {
		if (!hasUnknown(prefix)) {
			if (!hasUnknown(s))
				return Satisfiability.fromBoolean(s.startsWith(prefix));
			String known = knownPrefix(s);
			if (known.startsWith(prefix))
				return Satisfiability.SATISFIED;
			return prefix.startsWith(known) ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		}
		// the prefix starts with its known prefix
		String knownPrefix = knownPrefix(prefix);
		if (!hasUnknown(s))
			return s.startsWith(knownPrefix) ? Satisfiability.UNKNOWN : Satisfiability.NOT_SATISFIED;
		return prefixCompatible(knownPrefix(s), knownPrefix) ? Satisfiability.UNKNOWN
				: Satisfiability.NOT_SATISFIED;
	}

	private static Satisfiability endsWithString(
			String s,
			String suffix) {
		return startsWithString(new StringBuilder(s).reverse().toString(),
				new StringBuilder(suffix).reverse().toString());
	}

	private static Satisfiability containsString(
			String s,
			String searched) {
		if (searched.isEmpty())
			return Satisfiability.SATISFIED;
		if (!hasUnknown(searched)) {
			if (!hasUnknown(s))
				return Satisfiability.fromBoolean(s.contains(searched));
			for (String segment : s.split(Pattern.quote(UNKNOWN_STRING), -1))
				if (segment.contains(searched))
					return Satisfiability.SATISFIED;
			return Satisfiability.UNKNOWN;
		}
		if (!hasUnknown(s))
			// the known parts of the searched string appear in order
			return instanceOf(UNKNOWN_STRING + searched + UNKNOWN_STRING, s) ? Satisfiability.UNKNOWN
					: Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}
}

package it.unive.jlisa.analysis.value;

import it.unive.jlisa.lattices.ConstantValueIntIntervalAutomaton;
import it.unive.jlisa.program.operator.JavaStringAppendBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharArrayOperator;
import it.unive.jlisa.program.operator.JavaStringAppendCharOperator;
import it.unive.jlisa.program.operator.JavaStringAppendDoubleOperator;
import it.unive.jlisa.program.operator.JavaStringAppendFloatOperator;
import it.unive.jlisa.program.operator.JavaStringAppendIntOperator;
import it.unive.jlisa.program.operator.JavaStringAppendLongOperator;
import it.unive.jlisa.program.operator.JavaStringAppendObjectOperator;
import it.unive.jlisa.program.operator.JavaStringAppendStringOperator;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringConcatOperator;
import it.unive.jlisa.program.operator.JavaStringContainsOperator;
import it.unive.jlisa.program.operator.JavaStringEndsWithOperator;
import it.unive.jlisa.program.operator.JavaStringEqualsOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringIndexOfOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.operator.JavaStringReplaceOperator;
import it.unive.jlisa.program.operator.JavaStringReverseOperator;
import it.unive.jlisa.program.operator.JavaStringStartsWithOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringFromToOperator;
import it.unive.jlisa.program.operator.JavaStringSubstringOperator;
import it.unive.jlisa.program.operator.JavaStringToLowerCaseOperator;
import it.unive.jlisa.program.operator.JavaStringToUpperCaseOperator;
import it.unive.jlisa.program.operator.JavaStringTrimOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfBooleanOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfCharOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfIntOperator;
import it.unive.jlisa.program.operator.JavaStringValueOfLongOperator;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.string.tarsis.Tarsis;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.datastructures.automaton.Transition;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import it.unive.lisa.util.datastructures.regex.TopAtom;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
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
		if (oracle != null && oracle.hasWholeValueAnlysis()) {
			RegexAutomaton result = withNumbers(expression, pp, oracle);
			if (result != null)
				return result;
		}
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
		if (oracle != null && oracle.hasWholeValueAnlysis()) {
			RegexAutomaton result = withNumbers(expression, left, pp, oracle);
			if (result != null)
				return result;
		}
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
		if (oracle != null && oracle.hasWholeValueAnlysis()) {
			RegexAutomaton result = withNumbers(expression, left, middle, right, pp, oracle);
			if (result != null)
				return result;
		}
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
	 * Returns whether the strings of {@code small} are surely strings of
	 * {@code big}, as {@link #included(RegexAutomaton, RegexAutomaton)}, also
	 * for bottom automata. It is the order of the automata of string values,
	 * kept only if they recognize finite languages (see the constructor of
	 * {@link ConstantValueIntIntervalAutomaton}): the inclusion of LiSA would
	 * complement and determinize them.
	 *
	 * @param small the first automaton
	 * @param big   the second automaton
	 *
	 * @return {@code true} if the inclusion surely holds
	 */
	public static boolean leqAutomata(
			RegexAutomaton small,
			RegexAutomaton big) {
		if (small.isBottom())
			return true;
		if (big.isBottom())
			return false;
		return included(small, big);
	}

	/**
	 * Returns the least upper bound of two automata of string values: the top
	 * automaton if one of them is top, their least upper bound otherwise (as
	 * {@link ConstantValueIntIntervalAutomaton}).
	 *
	 * @param a the first automaton
	 * @param b the second automaton
	 *
	 * @return the least upper bound
	 *
	 * @throws SemanticException if the least upper bound fails
	 */
	public static RegexAutomaton lubAutomata(
			RegexAutomaton a,
			RegexAutomaton b)
			throws SemanticException {
		if (a.isBottom())
			return b;
		if (b.isBottom())
			return a;
		if (a == RegexAutomaton.TOP || b == RegexAutomaton.TOP)
			return RegexAutomaton.TOP;
		return a.lub(b);
	}

	/**
	 * Returns an over-approximation of the intersection of two automata of
	 * string values: the intersection of their languages when they are finite,
	 * one of them otherwise (as {@link ConstantValueIntIntervalAutomaton}).
	 *
	 * @param a the first automaton
	 * @param b the second automaton
	 *
	 * @return the over-approximation of the intersection
	 */
	public static RegexAutomaton glbAutomata(
			RegexAutomaton a,
			RegexAutomaton b) {
		if (a.isBottom() || b.isBottom())
			return a.isBottom() ? a : b;
		if (a == RegexAutomaton.TOP)
			return b;
		if (b == RegexAutomaton.TOP)
			return a;
		SortedSet<String> la = language(a);
		SortedSet<String> lb = language(b);
		if (la == null || lb == null)
			return la == null ? b : a;
		if (la.stream().anyMatch(JavaTarsis::hasUnknown) || lb.stream().anyMatch(JavaTarsis::hasUnknown))
			return a;
		SortedSet<String> common = new TreeSet<>(la);
		common.retainAll(lb);
		return fromLanguage(common);
	}

	/**
	 * Returns the widening of two automata of string values: a language that
	 * keeps growing goes to top (as {@link ConstantValueIntIntervalAutomaton}).
	 *
	 * @param previous the automaton of the previous iteration
	 * @param next     the automaton of the current iteration
	 *
	 * @return the widening
	 */
	public static RegexAutomaton wideningAutomata(
			RegexAutomaton previous,
			RegexAutomaton next) {
		if (previous.isBottom())
			return next;
		if (next.isBottom())
			return previous;
		return previous == RegexAutomaton.TOP || next == RegexAutomaton.TOP || !included(next, previous)
				? RegexAutomaton.TOP
				: previous;
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

	// ADAPTER TO JavaWholeValueAnalysis. As a participant of a whole-value
	// analysis, this domain does what ConstantPropagationWithTarsis does
	// between its two components, with the other participants in place of
	// ConstantPropagationWithIntervals: the integers and the booleans that
	// string operations take are read from the constraints of the other
	// participants, and the integers and booleans that string operations give
	// are given to them as constraints. The methods below follow the ones of
	// ConstantPropagationWithTarsis, with the same names.

	@Override
	public ValueEnvironment<RegexAutomaton> makeLattice() {
		return new Strings(top());
	}

	/**
	 * The environment of the automata of the string identifiers. As in
	 * {@link ConstantValueIntIntervalAutomaton}, an automaton is kept only if
	 * it recognizes a finite language that {@link #language(RegexAutomaton)}
	 * can enumerate (and it is not too large), otherwise it is replaced by the
	 * top automaton, and it is represented as the strings of its language (see
	 * {@link JavaTarsis#representation(RegexAutomaton)}).
	 */
	private static class Strings extends ValueEnvironment<RegexAutomaton> {

		private Strings(
				RegexAutomaton domain) {
			super(domain);
		}

		private Strings(
				RegexAutomaton domain,
				PatriciaTrieMap<Identifier, RegexAutomaton> function) {
			super(domain, function);
		}

		@Override
		public ValueEnvironment<RegexAutomaton> top() {
			return isTop() ? this : new Strings(lattice.top(), null);
		}

		@Override
		public ValueEnvironment<RegexAutomaton> bottom() {
			return isBottom() ? this : new Strings(lattice.bottom(), null);
		}

		@Override
		public ValueEnvironment<RegexAutomaton> mk(
				RegexAutomaton lattice,
				PatriciaTrieMap<Identifier, RegexAutomaton> function) {
			if (function != null)
				for (Map.Entry<Identifier, RegexAutomaton> entry : function.entrySet())
					if (!kept(entry.getValue()))
						function = function.put(entry.getKey(), RegexAutomaton.TOP);
			return new Strings(lattice, function);
		}

		// the operations of the automata follow the ones of
		// ConstantValueIntIntervalAutomaton (see leqAutomata, lubAutomata,
		// glbAutomata, wideningAutomata), the other parts are the ones of
		// FunctionalLattice

		@Override
		public boolean lessOrEqualAux(
				ValueEnvironment<RegexAutomaton> other)
				throws SemanticException {
			if (function != null)
				for (Identifier key : function.keySet()) {
					RegexAutomaton state = getState(key);
					if (state != null && !leqAutomata(state, other.getState(key)))
						return false;
				}
			return true;
		}

		@Override
		public ValueEnvironment<RegexAutomaton> lubAux(
				ValueEnvironment<RegexAutomaton> other)
				throws SemanticException {
			return functionalLift(other, lattice.bottom(), this::lubKeys,
					(
							a,
							b) -> a == null ? b : lubAutomata(a, b));
		}

		@Override
		public ValueEnvironment<RegexAutomaton> wideningAux(
				ValueEnvironment<RegexAutomaton> other)
				throws SemanticException {
			return functionalLift(other, lattice.bottom(), this::lubKeys,
					(
							a,
							b) -> a == null ? b : wideningAutomata(a, b));
		}

		@Override
		public ValueEnvironment<RegexAutomaton> glbAux(
				ValueEnvironment<RegexAutomaton> other)
				throws SemanticException {
			return functionalLift(other, lattice.top(), this::glbKeys,
					(
							a,
							b) -> a == null ? b : glbAutomata(a, b));
		}

		@Override
		public ValueEnvironment<RegexAutomaton> narrowingAux(
				ValueEnvironment<RegexAutomaton> other)
				throws SemanticException {
			// the automaton is kept: narrowing it through the intersection of
			// automata would not be sound (as
			// ConstantValueIntIntervalAutomaton)
			return functionalLift(other, lattice.top(), this::glbKeys,
					(
							a,
							b) -> a == null ? b : a);
		}

		// as the constructor of ConstantValueIntIntervalAutomaton
		private static boolean kept(
				RegexAutomaton automaton) {
			return automaton.isBottom() || automaton != RegexAutomaton.TOP
					&& automaton.getStates().size() <= ConstantValueIntIntervalAutomaton.MAX_STATES
					&& language(automaton) != null;
		}

		@Override
		public StructuredRepresentation representation() {
			if (isTop() || isBottom() || function == null)
				return super.representation();
			Map<StructuredRepresentation, StructuredRepresentation> map = new TreeMap<>();
			for (Map.Entry<Identifier, RegexAutomaton> entry : function)
				map.put(new StringRepresentation(entry.getKey()), JavaTarsis.representation(entry.getValue()));
			return new MapRepresentation(map);
		}
	}

	/**
	 * Yields the representation of the given automaton, listing its strings
	 * when the language is finite (converting the automaton to a regular
	 * expression may take exponential time).
	 *
	 * @param automaton the automaton
	 *
	 * @return the representation
	 */
	public static StructuredRepresentation representation(
			RegexAutomaton automaton) {
		SortedSet<String> lang = language(automaton);
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
	public boolean canProcess(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle) {
		try {
			if (isString(e, pp, oracle))
				return true;
			// the integers compared with the lengths of strings (see
			// refineLengths)
			if (e instanceof Constant c && (c.getValue() instanceof Number || c.getValue() instanceof Character))
				return true;
			// the integers and booleans given by string operations (see
			// constraints)
			if (e instanceof UnaryExpression u && u.getOperator() == JavaStringLengthOperator.INSTANCE)
				return isString(u.getExpression(), pp, oracle);
			if (e instanceof BinaryExpression b) {
				BinaryOperator operator = b.getOperator();
				if (operator == JavaStringCharAtOperator.INSTANCE
						|| operator == JavaStringIndexOfCharOperator.INSTANCE)
					return isString(b.getLeft(), pp, oracle);
				if (operator == JavaStringIndexOfOperator.INSTANCE || isPredicate(operator))
					return isString(b.getLeft(), pp, oracle) && isString(b.getRight(), pp, oracle);
			}
			return false;
		} catch (SemanticException ex) {
			return false;
		}
	}

	/**
	 * Yields whether the given expression is a string, that is, the automaton
	 * of its value is meaningful.
	 */
	private static boolean isString(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Type type = expression.getStaticType();
		if (type.isStringType())
			return true;
		if (!type.isUntyped())
			return false;
		Set<Type> types = oracle.getRuntimeTypesOf(expression, pp);
		return !types.isEmpty() && types.stream().allMatch(Type::isStringType);
	}

	@Override
	public ValueEnvironment<RegexAutomaton> assign(
			ValueEnvironment<RegexAutomaton> environment,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (oracle != null && oracle.hasWholeValueAnlysis() && !isString(id, pp, oracle))
			// not a string (e.g., n = s.length()): tracked by the other
			// participants
			return environment.knowsIdentifier(id) ? environment.forgetIdentifier(id, pp) : environment;
		return super.assign(environment, id, expression, pp, oracle);
	}

	// as evalUnaryExpression of ConstantPropagationWithTarsis: the operations
	// building strings from numbers and booleans, or null
	private RegexAutomaton withNumbers(
			UnaryExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!isString(expression, pp, oracle))
			return null;
		Operator operator = expression.getOperator();
		ValueExpression arg = (ValueExpression) expression.getExpression();
		if (operator == JavaStringValueOfIntOperator.INSTANCE || operator == JavaStringValueOfLongOperator.INSTANCE)
			return integers(interval(arg, pp, oracle));
		if (operator == JavaStringValueOfCharOperator.INSTANCE)
			return chars(interval(arg, pp, oracle));
		if (operator == JavaStringValueOfBooleanOperator.INSTANCE)
			return booleans(arg, pp, oracle);
		return null;
	}

	// as evalBinaryExpression of ConstantPropagationWithTarsis: the operations
	// on a string and a number or a boolean, or null
	private RegexAutomaton withNumbers(
			BinaryExpression expression,
			RegexAutomaton left,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!isString(expression, pp, oracle) || !isString(expression.getLeft(), pp, oracle))
			return null;
		BinaryOperator operator = expression.getOperator();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (operator == JavaStringSubstringOperator.INSTANCE)
			return substring(left, interval(right, pp, oracle), null);
		if (operator == JavaStringAppendCharOperator.INSTANCE)
			return left.concat(chars(interval(right, pp, oracle)));
		if (operator == JavaStringAppendIntOperator.INSTANCE || operator == JavaStringAppendLongOperator.INSTANCE)
			return left.concat(integers(interval(right, pp, oracle)));
		if (operator == JavaStringAppendBooleanOperator.INSTANCE)
			return left.concat(booleans(right, pp, oracle));
		return null;
	}

	// as evalTernaryExpression of ConstantPropagationWithTarsis: the
	// operations on a string and numbers or characters, or null
	private RegexAutomaton withNumbers(
			TernaryExpression expression,
			RegexAutomaton left,
			RegexAutomaton middle,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!isString(expression, pp, oracle) || !isString(expression.getLeft(), pp, oracle))
			return null;
		Operator operator = expression.getOperator();
		if (operator == JavaStringSubstringFromToOperator.INSTANCE)
			return substring(left, interval((ValueExpression) expression.getMiddle(), pp, oracle),
					interval((ValueExpression) expression.getRight(), pp, oracle));
		if (operator == JavaStringReplaceOperator.INSTANCE)
			return replace(left, stringOrChar(expression.getMiddle(), middle, pp, oracle),
					stringOrChar(expression.getRight(), right, pp, oracle));
		return null;
	}

	private RegexAutomaton stringOrChar(
			SymbolicExpression expression,
			RegexAutomaton value,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isString(expression, pp, oracle))
			return value;
		return chars(interval((ValueExpression) expression, pp, oracle));
	}

	private RegexAutomaton booleans(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		SortedSet<String> result = new TreeSet<>();
		Boolean b = bool(expression, pp, oracle);
		if (b != null)
			result.add(String.valueOf(b));
		else {
			result.add("true");
			result.add("false");
		}
		return fromLanguage(result);
	}

	@Override
	public RegexAutomaton evalTypeCast(
			BinaryExpression cast,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (oracle != null && oracle.hasWholeValueAnlysis())
			// as evalTypeCast of ConstantPropagationWithTarsis
			return isString(cast.getLeft(), pp, oracle) ? left : top();
		return super.evalTypeCast(cast, left, right, pp, oracle);
	}

	@Override
	public RegexAutomaton evalTypeConv(
			BinaryExpression conv,
			RegexAutomaton left,
			RegexAutomaton right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (oracle != null && oracle.hasWholeValueAnlysis())
			// as evalTypeConv of ConstantPropagationWithTarsis
			return isString(conv.getLeft(), pp, oracle) ? left : top();
		return super.evalTypeConv(conv, left, right, pp, oracle);
	}

	// the value of a boolean, as known by the other participants, or null
	private Boolean bool(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (e instanceof Constant c && c.getValue() instanceof Boolean b)
			return b;
		Set<BinaryExpression> constraints = oracle.constraints(this, e, pp);
		if (constraints != null)
			for (BinaryExpression c : constraints)
				if (c.getOperator() == ComparisonEq.INSTANCE && c.getLeft() instanceof Constant k
						&& k.getValue() instanceof Boolean b)
					return b;
		return null;
	}

	// the interval of an integer (or of the code of a character), as known by
	// the other participants: the value of a constant, or a range with both
	// bounds (in ConstantPropagationWithTarsis, the interval of the first
	// component)
	private IntInterval interval(
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (e instanceof Constant c) {
			Object v = c.getValue();
			if (v instanceof Character ch)
				return new IntInterval((int) ch, (int) ch);
			if (v instanceof Integer || v instanceof Short || v instanceof Byte)
				return new IntInterval(((Number) v).intValue(), ((Number) v).intValue());
			if (v instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE)
				return new IntInterval(l.intValue(), l.intValue());
			return IntInterval.INFINITY;
		}
		Set<BinaryExpression> constraints = oracle.constraints(this, e, pp);
		if (constraints == null)
			return IntInterval.INFINITY;
		Long ge = null, le = null;
		for (BinaryExpression c : constraints)
			if (c.getLeft() instanceof Constant k) {
				Object v = k.getValue();
				if (v instanceof Character ch)
					v = (int) ch;
				if (!(v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte))
					continue;
				long n = ((Number) v).longValue();
				if (c.getOperator() == ComparisonEq.INSTANCE)
					return new IntInterval(new MathNumber(n), new MathNumber(n));
				// low <= e and high >= e (see ValueDomain.makeRangeConstraints)
				if (c.getOperator() == ComparisonLe.INSTANCE)
					ge = n;
				else if (c.getOperator() == ComparisonGe.INSTANCE)
					le = n;
			}
		if (ge == null || le == null)
			return IntInterval.INFINITY;
		return new IntInterval(new MathNumber(Math.min(ge, le)), new MathNumber(Math.max(ge, le)));
	}

	// as refineInteger and evalBinaryExpression of
	// ConstantPropagationWithTarsis:
	// the integers and the booleans given by string operations, and the
	// string of an automaton with a single one (as in reduce)
	@Override
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			ValueEnvironment<RegexAutomaton> state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return null;
		if (isString(e, pp, oracle)) {
			String single = singleString(eval(state, e, pp, oracle));
			if (single != null)
				return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getStringType(), single, e, pp);
			return Collections.emptySet();
		}
		IntInterval result = null;
		if (e instanceof UnaryExpression u && u.getOperator() == JavaStringLengthOperator.INSTANCE
				&& isString(u.getExpression(), pp, oracle))
			result = length(eval(state, (ValueExpression) u.getExpression(), pp, oracle));
		else if (e instanceof BinaryExpression b) {
			BinaryOperator operator = b.getOperator();
			ValueExpression left = (ValueExpression) b.getLeft();
			ValueExpression right = (ValueExpression) b.getRight();
			boolean stringLeft = isString(left, pp, oracle);
			boolean stringRight = isString(right, pp, oracle);
			if (isPredicate(operator) && stringLeft && stringRight) {
				Satisfiability sat = satisfiesPredicate(operator, eval(state, left, pp, oracle),
						eval(state, right, pp, oracle));
				if (sat == Satisfiability.SATISFIED || sat == Satisfiability.NOT_SATISFIED)
					return ValueDomain.makeEqConstraint(pp.getProgram().getTypes().getBooleanType(),
							sat == Satisfiability.SATISFIED, e, pp);
				return Collections.emptySet();
			}
			if (operator == JavaStringCharAtOperator.INSTANCE && stringLeft)
				// characters are kept as intervals of codes
				result = charAt(eval(state, left, pp, oracle), interval(right, pp, oracle));
			else if (operator == JavaStringIndexOfOperator.INSTANCE && stringLeft && stringRight)
				result = indexOf(eval(state, left, pp, oracle), eval(state, right, pp, oracle));
			else if (operator == JavaStringIndexOfCharOperator.INSTANCE && stringLeft)
				result = indexOf(eval(state, left, pp, oracle), chars(interval(right, pp, oracle)));
		}
		if (result == null || result.isBottom() || result.isTop())
			return Collections.emptySet();
		// low <= e and high >= e, as the intervals read them, with int
		// constants
		Set<BinaryExpression> constraints = new HashSet<>();
		Type type = pp.getProgram().getTypes().getIntegerType();
		Long low = result.lowIsMinusInfinity() ? null : bound(result.getLow());
		Long high = result.highIsPlusInfinity() ? null : bound(result.getHigh());
		if (low != null && low >= Integer.MIN_VALUE && low <= Integer.MAX_VALUE)
			constraints.addAll(ValueDomain.makeConstraint(type, (int) (long) low, ComparisonLe.INSTANCE, e, pp));
		if (high != null && high >= Integer.MIN_VALUE && high <= Integer.MAX_VALUE)
			constraints.addAll(ValueDomain.makeConstraint(type, (int) (long) high, ComparisonGe.INSTANCE, e, pp));
		return constraints;
	}

	// as assume of ConstantPropagationWithTarsis, for the automata only: the
	// first component (the other participants) assumes the expression by
	// itself, asking the bounds of the string operations through constraints
	@Override
	public ValueEnvironment<RegexAutomaton> assume(
			ValueEnvironment<RegexAutomaton> environment,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (oracle == null || !oracle.hasWholeValueAnlysis())
			return super.assume(environment, expression, src, dest, oracle);
		if (environment.isBottom())
			return environment;
		ValueExpression e = expression.removeNegations();
		Map<Identifier, RegexAutomaton> refined = new HashMap<>();
		if (!refineStrings(environment, e, refined, src, oracle)
				|| !refineLengths(environment, e, refined, src, oracle))
			return environment.bottom();
		ValueEnvironment<RegexAutomaton> result = environment;
		for (Map.Entry<Identifier, RegexAutomaton> entry : refined.entrySet())
			result = result.putState(entry.getKey(), entry.getValue());
		return result;
	}

	/**
	 * Refines the automata of the string identifiers of the predicates on
	 * strings inside {@code expression} (and of its conjunctions and
	 * disjunctions), assuming that {@code expression} holds.
	 *
	 * @return {@code false} if the expression cannot hold
	 */
	private boolean refineStrings(
			ValueEnvironment<RegexAutomaton> environment,
			ValueExpression expression,
			Map<Identifier, RegexAutomaton> refined,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof BinaryExpression binary && binary.getOperator() == LogicalAnd.INSTANCE)
			return refineStrings(environment, (ValueExpression) binary.getLeft(), refined, pp, oracle)
					&& refineStrings(environment, (ValueExpression) binary.getRight(), refined, pp, oracle);

		if (expression instanceof BinaryExpression binary && binary.getOperator() == LogicalOr.INSTANCE) {
			Map<Identifier, RegexAutomaton> left = new HashMap<>(refined);
			Map<Identifier, RegexAutomaton> right = new HashMap<>(refined);
			boolean l = refineStrings(environment, (ValueExpression) binary.getLeft(), left, pp, oracle);
			boolean r = refineStrings(environment, (ValueExpression) binary.getRight(), right, pp, oracle);
			if (!l && !r)
				return false;
			refined.clear();
			if (!l)
				refined.putAll(right);
			else if (!r)
				refined.putAll(left);
			else
				// an identifier refined by one side only keeps its value
				for (Identifier id : left.keySet())
					if (right.containsKey(id))
						refined.put(id, left.get(id).lub(right.get(id)));
			return true;
		}

		boolean positive = true;
		ValueExpression leaf = expression;
		if (expression instanceof UnaryExpression unary && unary.getOperator() == LogicalNegation.INSTANCE) {
			positive = false;
			leaf = (ValueExpression) unary.getExpression();
		}
		if (!(leaf instanceof BinaryExpression binary) || !isPredicate(binary.getOperator()))
			return true;
		ValueExpression left = (ValueExpression) binary.getLeft();
		ValueExpression right = (ValueExpression) binary.getRight();
		if (!isString(left, pp, oracle) || !isString(right, pp, oracle))
			return true;
		RegexAutomaton leftAutomaton = automatonOf(environment, refined, left, pp, oracle);
		RegexAutomaton rightAutomaton = automatonOf(environment, refined, right, pp, oracle);
		for (boolean refineLeft : new boolean[] { true, false }) {
			ValueExpression target = refineLeft ? left : right;
			if (!(target instanceof Identifier id) || !id.canBeAssigned() || id.isWeak())
				continue;
			RegexAutomaton result = refine(binary.getOperator(), leftAutomaton, rightAutomaton, refineLeft, positive);
			if (result == null)
				continue;
			if (result.isBottom())
				return false;
			refined.put(id, result);
		}
		return true;
	}

	/**
	 * Refines the automata of the string identifiers whose length is compared
	 * inside {@code expression} (and of its conjunctions), assuming that
	 * {@code expression} holds.
	 *
	 * @return {@code false} if the expression cannot hold
	 */
	private boolean refineLengths(
			ValueEnvironment<RegexAutomaton> environment,
			ValueExpression expression,
			Map<Identifier, RegexAutomaton> refined,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!(expression instanceof BinaryExpression binary))
			return true;
		BinaryOperator operator = binary.getOperator();
		ValueExpression left = (ValueExpression) binary.getLeft();
		ValueExpression right = (ValueExpression) binary.getRight();
		if (operator == LogicalAnd.INSTANCE)
			return refineLengths(environment, left, refined, pp, oracle)
					&& refineLengths(environment, right, refined, pp, oracle);

		boolean lengthLeft = isLengthOf(left);
		if (!lengthLeft && !isLengthOf(right))
			return true;
		Identifier id = (Identifier) ((UnaryExpression) (lengthLeft ? left : right)).getExpression();
		IntInterval other = interval(lengthLeft ? right : left, pp, oracle);
		if (other.isBottom())
			return true;
		// the comparison is turned into length op other
		if (!lengthLeft)
			operator = operator == ComparisonLt.INSTANCE ? ComparisonGt.INSTANCE
					: operator == ComparisonLe.INSTANCE ? ComparisonGe.INSTANCE
							: operator == ComparisonGt.INSTANCE ? ComparisonLt.INSTANCE
									: operator == ComparisonGe.INSTANCE ? ComparisonLe.INSTANCE : operator;
		Long low = other.lowIsMinusInfinity() ? null : bound(other.getLow());
		Long high = other.highIsPlusInfinity() ? null : bound(other.getHigh());
		IntInterval length;
		if (operator == ComparisonLt.INSTANCE)
			length = interval(null, high == null ? null : high - 1);
		else if (operator == ComparisonLe.INSTANCE)
			length = interval(null, high);
		else if (operator == ComparisonGt.INSTANCE)
			length = interval(low == null ? null : low + 1, null);
		else if (operator == ComparisonGe.INSTANCE)
			length = interval(low, null);
		else if (operator == ComparisonEq.INSTANCE)
			length = interval(low, high);
		else
			return true;
		if (length == null)
			return true;
		if (length.isBottom())
			return false;
		RegexAutomaton result = refineLength(automatonOf(environment, refined, id, pp, oracle), length);
		if (result == null)
			return true;
		if (result.isBottom())
			return false;
		refined.put(id, result);
		return true;
	}

	private static boolean isLengthOf(
			ValueExpression expression) {
		return expression instanceof UnaryExpression unary
				&& unary.getOperator() == JavaStringLengthOperator.INSTANCE
				&& unary.getExpression() instanceof Identifier id && id.canBeAssigned() && !id.isWeak();
	}

	private static Long bound(
			MathNumber n) {
		try {
			return n.toLong();
		} catch (MathNumberConversionException e) {
			return null;
		}
	}

	/**
	 * Yields the interval of the lengths between the given bounds (where
	 * {@code null} is unbounded), or {@code null} if it is not bounded.
	 */
	private static IntInterval interval(
			Long low,
			Long high) {
		long l = low == null ? 0 : Math.max(0, low);
		if (high != null && high < l)
			return IntInterval.BOTTOM;
		if (high == null && l == 0)
			return null;
		if (l > Integer.MAX_VALUE || (high != null && high > Integer.MAX_VALUE))
			return null;
		return new IntInterval(Integer.valueOf((int) l), high == null ? null : Integer.valueOf((int) (long) high));
	}

	private RegexAutomaton automatonOf(
			ValueEnvironment<RegexAutomaton> environment,
			Map<Identifier, RegexAutomaton> refined,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof Identifier id && refined.containsKey(id))
			return refined.get(id);
		return eval(environment, expression, pp, oracle);
	}
}

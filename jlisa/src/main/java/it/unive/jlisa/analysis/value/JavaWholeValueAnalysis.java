package it.unive.jlisa.analysis.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValue;
import it.unive.lisa.analysis.combination.constraints.WholeValueAnalysis;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.ComparisonOperator;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The whole-value analysis of LiSA, where each participant assumes only the
 * conditions whose operands it can process, answers only on the expressions
 * whose operands it can process (e.g., the intervals decide {@code x < 10}),
 * and forgets the identifiers assigned with expressions it cannot process. In
 * the analysis of LiSA, every participant assumes every condition, and a bottom
 * state of one of them makes the whole state bottom: e.g., the intervals of
 * {@code x != null} are bottom (a reference and null have no interval), and the
 * branch where {@code x} is not null becomes unreachable.<br>
 * <br>
 * In the analysis of LiSA, each participant tracks one kind of values (e.g.,
 * the strings, or the numbers), and a state is bottom only if all the
 * participants are bottom. Here, several participants track the same values
 * (e.g., the constants, the intervals and the kinds of floating point values
 * all track the numbers), hence what a participant does not process must not
 * reach it. It diverges from the base WholeValueAnalysis in:
 * <ul>
 * <li>{@link #assign}: a participant that cannot process the expression forgets
 * the identifier (LiSA keeps its old value, which is no longer true);</li>
 * <li>{@link #assume}: a condition goes only to the participants that process
 * its operands, and conjunctions are split among them (LiSA gives every
 * condition to every participant); as in LiSA, a bottom state of one of them
 * makes the whole state bottom, which is sound since it processes the
 * condition;</li>
 * <li>{@link #satisfies}: only the participants that process the operands
 * answer, connectives and negations are decided from their parts, and a bottom
 * answer of a participant in a reachable state is ignored (a value it cannot
 * represent, e.g., NaN);</li>
 * <li>{@link #constraints}: only the participants that process the expression,
 * other than the asking one, give constraints.</li>
 * </ul>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class JavaWholeValueAnalysis
		extends
		WholeValueAnalysis {

	/**
	 * Builds the analysis.
	 *
	 * @param participants the participants
	 */
	public JavaWholeValueAnalysis(
			ValueDomain<?>... participants) {
		super(participants);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue assign(
			WholeValue state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++)
			if (participants[i].canProcess(expression, pp, oracle))
				lattices[i] = (ValueLattice<?>) ((ValueDomain) participants[i]).assign(
						state.get(i),
						id,
						expression,
						pp,
						oracle);
			else if (state.get(i).knowsIdentifier(id))
				// the old value of id is no longer true (LiSA keeps it)
				lattices[i] = (ValueLattice<?>) state.get(i).forgetIdentifier(id, pp);
			else
				lattices[i] = state.get(i);
		return new WholeValue(lattices);
	}

	@Override
	public WholeValue smallStepSemantics(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression instanceof Skip)
			// no value (e.g., the result of a void call): no participant has
			// anything to do, and asking them whether they can process it
			// would ask the type domain for the types of no value
			return state;
		return super.smallStepSemantics(state, expression, pp, oracle);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue assume(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		// a condition known to be false makes the branch unreachable, even if
		// no domain can represent its negation (e.g., z != 0 with z == 0: the
		// intervals do not refine !=)
		Satisfiability known = satisfies(state, expression, src, oracle);
		if (known == Satisfiability.NOT_SATISFIED || known == Satisfiability.BOTTOM)
			return state.bottom();
		ValueExpression[] conjuncts = conjuncts(expression);
		if (conjuncts != null) {
			// each conjunct goes to the participants that process its
			// operands, even if they do not process the other one (e.g.,
			// s != null && s.contains(t))
			WholeValue result = assume(state, conjuncts[0], src, dest, oracle);
			return result.isBottom() ? result : assume(result, conjuncts[1], src, dest, oracle);
		}
		ValueLattice<?>[] lattices = new ValueLattice<?>[participants.length];
		for (int i = 0; i < participants.length; i++) {
			if (!processesOperands(participants[i], expression, src, oracle)) {
				// the condition is not about values of this participant (LiSA
				// gives every condition to every participant)
				lattices[i] = state.get(i);
				continue;
			}
			lattices[i] = (ValueLattice<?>) ((ValueDomain) participants[i]).assume(
					state.get(i),
					expression,
					src,
					dest,
					oracle);
			if (lattices[i].isBottom())
				// the participant processes the condition, which cannot hold
				return state.bottom();
		}
		return new WholeValue(lattices);
	}

	// the negation of the given expression, without double negations
	private static ValueExpression negation(
			ValueExpression e,
			UnaryExpression origin) {
		if (e instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof ValueExpression inner)
			return inner;
		return new UnaryExpression(origin.getStaticType(), e, LogicalNegation.INSTANCE, origin.getCodeLocation());
	}

	// the two conjuncts of the given condition, if it is a conjunction or the
	// negation of a disjunction, null otherwise
	private static ValueExpression[] conjuncts(
			ValueExpression expression) {
		if (expression instanceof BinaryExpression b && b.getOperator() == LogicalAnd.INSTANCE
				&& b.getLeft() instanceof ValueExpression l && b.getRight() instanceof ValueExpression r)
			return new ValueExpression[] { l, r };
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof BinaryExpression b && b.getOperator() == LogicalOr.INSTANCE
				&& b.getLeft() instanceof ValueExpression l && b.getRight() instanceof ValueExpression r)
			return new ValueExpression[] { negation(l, u), negation(r, u) };
		return null;
	}

	// whether the participant can process the operands of the comparisons in
	// the given condition (through negations and connectives)
	private static boolean processesOperands(
			ValueDomain<?> participant,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof ValueExpression inner)
			return processesOperands(participant, inner, pp, oracle);
		if (expression instanceof BinaryExpression b && b.getLeft() instanceof ValueExpression l
				&& b.getRight() instanceof ValueExpression r) {
			if (b.getOperator() == LogicalAnd.INSTANCE || b.getOperator() == LogicalOr.INSTANCE)
				return processesOperands(participant, l, pp, oracle) && processesOperands(participant, r, pp, oracle);
			if (b.getOperator() instanceof ComparisonOperator)
				return participant.canProcess(l, pp, oracle) && participant.canProcess(r, pp, oracle);
		}
		return participant.canProcess(expression, pp, oracle);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Satisfiability satisfies(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		// connectives and negations are decided here, each comparison by the
		// participants that process its operands (e.g., in s != null &&
		// s.contains(t), the second comparison may be false even if no
		// participant processes null)
		if (expression instanceof UnaryExpression u && u.getOperator() == LogicalNegation.INSTANCE
				&& u.getExpression() instanceof ValueExpression inner)
			return satisfies(state, inner, pp, oracle).negate();
		if (expression instanceof BinaryExpression b && b.getLeft() instanceof ValueExpression l
				&& b.getRight() instanceof ValueExpression r) {
			if (b.getOperator() == LogicalAnd.INSTANCE || b.getOperator() == LogicalOr.INSTANCE) {
				Satisfiability split = b.getOperator() == LogicalAnd.INSTANCE
						? satisfies(state, l, pp, oracle).and(satisfies(state, r, pp, oracle))
						: satisfies(state, l, pp, oracle).or(satisfies(state, r, pp, oracle));
				if (split != Satisfiability.UNKNOWN)
					return split;
				// a domain may decide the whole connective even if it decides
				// none of its operands (e.g., x == 0 || x == 1 with x in {0,
				// 1})
				return whole(state, expression, pp, oracle);
			}
		}
		return whole(state, expression, pp, oracle);
	}

	// the satisfiability of the given expression, as the glb of the answers of
	// the participants processing its operands
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Satisfiability whole(
			WholeValue state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability result = null;
		for (int i = 0; i < participants.length; i++) {
			if (!processesOperands(participants[i], expression, pp, oracle))
				// its answer would not be about the operands of expression
				// (LiSA asks every participant)
				continue;
			Satisfiability res = ((ValueDomain) participants[i]).satisfies(state.get(i), expression, pp, oracle);
			if (res == Satisfiability.BOTTOM) {
				if (state.get(i).isBottom())
					return Satisfiability.BOTTOM;
				// a bottom value in a reachable state: the participant cannot
				// represent the value of an operand (e.g., NaN), which says
				// nothing about the reachability of the state (LiSA answers
				// BOTTOM)
				continue;
			}
			result = result == null ? res : result.glb(res);
		}
		return result == null ? Satisfiability.UNKNOWN : result;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set<BinaryExpression> constraints(
			ValueDomain<?> requesting,
			WholeValue state,
			ValueExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return null;
		if (state.isTop())
			return Collections.emptySet();
		Set<BinaryExpression> result = new HashSet<>();
		for (int i = 0; i < participants.length; i++) {
			if (participants[i] == requesting || !processesOperands(participants[i], e, pp, oracle))
				// its constraints would not be about the value of e (e.g.,
				// the intervals of x != null; LiSA asks every participant)
				continue;
			Set<BinaryExpression> c = ((ValueDomain) participants[i]).constraints(requesting, state.get(i), e, pp,
					oracle);
			if (c != null)
				result.addAll(c);
		}
		return result;
	}
}

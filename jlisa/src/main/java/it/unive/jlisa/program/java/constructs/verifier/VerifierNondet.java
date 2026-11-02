package it.unive.jlisa.program.java.constructs.verifier;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Map;
import java.util.Set;

/**
 * The semantics of the methods of {@code org.sosy_lab.sv_benchmarks.Verifier}
 * that return a nondeterministic value of a primitive type or a string
 * ({@code nondetInt()}, {@code nondetString()}, ...). Their bodies in the
 * SV-COMP benchmarks only approximate them through {@link java.util.Random},
 * and "do NOT define the verification semantics": the frontend replaces them
 * with this construct, that yields any value of the return type of the method
 * (any non-null string for {@code nondetString()}). Since the methods become
 * native code members, their results are inputs of the program, that the
 * verification of warnings can assign to reproduce a warning.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class VerifierNondet extends NaryExpression implements PluggableStatement {

	/**
	 * The name of the class of the SV-COMP benchmarks defining the methods.
	 */
	public static final String VERIFIER_CLASS = "org.sosy_lab.sv_benchmarks.Verifier";

	/**
	 * The name of the method returning a nondeterministic string.
	 */
	public static final String NONDET_STRING = "nondetString";

	/**
	 * The methods whose results are primitive values, with the type of their
	 * results.
	 */
	private static final Map<String, Type> PRIMITIVES = Map.of(
			"nondetBoolean", JavaBooleanType.INSTANCE,
			"nondetByte", JavaByteType.INSTANCE,
			"nondetChar", JavaCharType.INSTANCE,
			"nondetShort", JavaShortType.INSTANCE,
			"nondetInt", JavaIntType.INSTANCE,
			"nondetLong", JavaLongType.INSTANCE,
			"nondetFloat", JavaFloatType.INSTANCE,
			"nondetDouble", JavaDoubleType.INSTANCE);

	/**
	 * The bounds of the integral results, as {@code [min, max]}.
	 */
	private static final Map<String, Number[]> BOUNDS = Map.of(
			"nondetByte", new Number[] { (int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE },
			"nondetChar", new Number[] { (int) Character.MIN_VALUE, (int) Character.MAX_VALUE },
			"nondetShort", new Number[] { (int) Short.MIN_VALUE, (int) Short.MAX_VALUE },
			"nondetInt", new Number[] { Integer.MIN_VALUE, Integer.MAX_VALUE },
			"nondetLong", new Number[] { Long.MIN_VALUE, Long.MAX_VALUE });

	private Statement originating;

	/**
	 * Yields whether the method with the given name, defined in the unit with
	 * the given name and with the given number of parameters, is replaced by
	 * this construct.
	 *
	 * @param unit       the name of the unit defining the method
	 * @param method     the name of the method
	 * @param parameters the number of parameters of the method
	 *
	 * @return whether the method is replaced by this construct
	 */
	public static boolean replaces(
			String unit,
			String method,
			int parameters) {
		return VERIFIER_CLASS.equals(unit)
				&& parameters == 0
				&& (PRIMITIVES.containsKey(method) || NONDET_STRING.equals(method));
	}

	/**
	 * Builds the construct.
	 *
	 * @param cfg      the {@link CFG} where this construct lies
	 * @param location the location where this construct is defined
	 */
	public VerifierNondet(
			CFG cfg,
			CodeLocation location) {
		super(cfg, location, "nondet");
	}

	/**
	 * Builds the construct, as {@link it.unive.lisa.program.cfg.NativeCFG}s
	 * do.
	 *
	 * @param cfg      the {@link CFG} where this construct lies
	 * @param location the location where this construct is defined
	 * @param params   the parameters of the call (none)
	 *
	 * @return the construct
	 */
	public static VerifierNondet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new VerifierNondet(cfg, location);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	private String method() {
		return originating instanceof Call ? ((Call) originating).getTargetName() : null;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					ExpressionSet[] params,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		String method = method();

		if (NONDET_STRING.equals(method))
			return nondetString(interprocedural, state, expressions);

		Type type = PRIMITIVES.get(method);
		if (type == null)
			// not one of the methods replaced by this construct
			return analysis.smallStepSemantics(state, new PushAny(Untyped.INSTANCE, getLocation()), context,
					originating);

		Number[] bounds = BOUNDS.get(method);
		if (bounds == null)
			return analysis.smallStepSemantics(state, new PushAny(type, getLocation()), context, originating);

		// constraints are given as "constant OP value"
		Constant min = new Constant(type, bounds[0], getLocation());
		Constant max = new Constant(type, bounds[1], getLocation());
		BinaryExpression lower = new BinaryExpression(type, min, min, ComparisonLe.INSTANCE, getLocation());
		BinaryExpression upper = new BinaryExpression(type, max, max, ComparisonGe.INSTANCE, getLocation());
		return analysis.smallStepSemantics(state,
				new PushFromConstraints(type, getLocation(), Set.of(lower, upper)), context, originating);
	}

	private <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> nondetString(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());

		// a new string, with any value
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(stringType), new Expression[0]);
		AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
				expressions);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild access = new AccessChild(stringType, ref, value, getLocation());
			result = result.lub(analysis.assign(callState, access, new PushAny(stringType, getLocation()), context,
					originating));
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return result.withExecutionExpressions(callState.getExecutionExpressions());
	}
}

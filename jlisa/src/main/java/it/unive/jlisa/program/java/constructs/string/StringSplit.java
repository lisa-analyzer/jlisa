package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.jlisa.program.cfg.SyntheticCodeLocation;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

public class StringSplit extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringSplit(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "split", left, right);
	}

	public static StringSplit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringSplit(cfg, location, params[0], params[1]);
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

	@Override
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		JavaReferenceType stringArrayType = JavaArrayType.getStringArray();
		JavaClassType stringType = JavaClassType.getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		// the result is computed if the string and the regular expression are
		// known
		String receiver = constant(interprocedural, state, left);
		String regex = constant(interprocedural, state, right);
		if (receiver != null && regex != null) {
			String[] parts;
			try {
				parts = receiver.split(regex);
			} catch (PatternSyntaxException e) {
				parts = null;
			}
			if (parts != null && parts.length <= MAX_PARTS)
				return split(interprocedural, state, parts, expressions);
		}

		// FIXME: this is a temporary solution: we return an array with length
		// set to top but with
		// for elements, set to the top string

		String src = ((SourceCodeLocation) getLocation()).getSourceFile();
		Expression unknown = stringArrayType.unknownValue(getCFG(), originating.getLocation());
		AnalysisState<A> splitArray = unknown.forwardSemantics(state, interprocedural, expressions);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression spl : splitArray.getExecutionExpressions()) {
			AccessChild ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 0, getLocation()), getLocation());

			// the 'value' property of strings
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", originating.getLocation());

			// first
			Expression unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 1),
					new JavaReferenceType(stringType), new Expression[0]);

			AnalysisState<A> unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			SymbolicExpression topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny()
					.get();
			AccessChild access = new AccessChild(stringType, topStringRef, var, getLocation());
			AnalysisState<
					A> sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), context,
							this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, context, originating));

			// second
			ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 1, getLocation()), getLocation());
			unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 2),
					new JavaReferenceType(stringType), new Expression[0]);

			unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny().get();
			access = new AccessChild(stringType, topStringRef, var, getLocation());
			sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), context, this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, context, originating));

			// third
			ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 2, getLocation()), getLocation());
			unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 3),
					new JavaReferenceType(stringType), new Expression[0]);

			unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny().get();
			access = new AccessChild(stringType, topStringRef, var, getLocation());
			sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), context, this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, context, originating));

			// forth
			ch = new AccessChild(stringType, spl,
					new Constant(JavaIntType.INSTANCE, 3, getLocation()), getLocation());
			unknownStr = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 4),
					new JavaReferenceType(stringType), new Expression[0]);

			unknownStrState = unknownStr.forwardSemantics(splitArray, interprocedural, expressions);
			topStringRef = unknownStrState.getExecutionExpressions().elements().stream().findAny().get();
			access = new AccessChild(stringType, topStringRef, var, getLocation());
			sem = analysis.assign(unknownStrState, access, new PushAny(stringType, getLocation()), context, this);

			result = result.lub(analysis.assign(sem, ch,
					topStringRef, context, originating));

		}

		return result.withExecutionExpressions(splitArray.getExecutionExpressions());
	}

	/**
	 * The maximum number of parts of a split string that is computed.
	 */
	private static final int MAX_PARTS = 64;

	/**
	 * Yields the array of the given parts of a split string: a new array of
	 * strings whose cells are new strings with the given contents.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> split(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			String[] parts,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		JavaReferenceType refType = JavaArrayType.getStringArray();
		Type stringType = getProgram().getTypes().getStringType();
		String src = ((SourceCodeLocation) getLocation()).getSourceFile();

		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());
		AnalysisState<A> result = analysis.smallStepSemantics(state, created, context, this);
		result = analysis.assign(result, array, new HeapReference(refType, created, getLocation()), context, this);
		// the cells and the length are accessed as JavaArrayAccess does
		HeapDereference container = new HeapDereference(refType.getInnerType(), array, getLocation());
		result = analysis.assign(result,
				new AccessChild(JavaIntType.INSTANCE, container,
						new Variable(JavaIntType.INSTANCE, "length", getLocation()), getLocation()),
				new Constant(JavaIntType.INSTANCE, parts.length, getLocation()), context, this);

		GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		for (int i = 0; i < parts.length; i++) {
			Expression str = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, i + 1),
					new JavaReferenceType(stringType), new Expression[0]);
			AnalysisState<A> allocated = str.forwardSemantics(result, interprocedural, expressions);
			AnalysisState<A> next = allocated.bottomExecution();
			for (SymbolicExpression ref : allocated.getExecutionExpressions()) {
				AnalysisState<A> written = analysis.assign(allocated,
						new AccessChild(stringType, ref, value, getLocation()),
						new Constant(stringType, parts[i], getLocation()), context, this);
				next = next.lub(analysis.assign(written,
						new AccessChild(stringType, container, new Constant(JavaIntType.INSTANCE, i, getLocation()),
								getLocation()),
						ref, context, this));
			}
			result = next;
		}

		getMetaVariables().add(array);
		return analysis.smallStepSemantics(result, array, context, this);
	}

	/**
	 * Yields the contents of the string referenced by the given expression,
	 * if the value domain knows that they are a single constant, or
	 * {@code null}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> String constant(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression reference)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type stringType = getProgram().getTypes().getStringType();
		AccessChild contents = new AccessChild(stringType, new HeapDereference(stringType, reference, getLocation()),
				new GlobalVariable(Untyped.INSTANCE, "value", getLocation()), getLocation());
		// the value domain and its state are retrieved without depending on
		// how the domain of the analysis is built
		ValueDomain vdom = analysis.domain.getDomainInstance(ValueDomain.class);
		ValueLattice env = ConstraintUtils.valueLattice(vdom, state.getExecutionState());
		if (vdom == null || env == null)
			return null;
		SemanticOracle oracle = analysis.domain.makeOracle(state.getExecutionState(), context);
		ExpressionSet rewritten = analysis.rewrite(state, contents, context, this);
		if (rewritten.size() != 1 || !(rewritten.iterator().next() instanceof ValueExpression))
			return null;
		Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints = vdom.constraints(null, env,
				(ValueExpression) rewritten.iterator().next(), this, oracle);
		if (constraints == null)
			return null;
		for (it.unive.lisa.symbolic.value.BinaryExpression c : constraints)
			if (c.getOperator() == ComparisonEq.INSTANCE && c.getLeft() instanceof Constant
					&& ((Constant) c.getLeft()).getValue() instanceof String)
				return (String) ((Constant) c.getLeft()).getValue();
		return null;
	}
}

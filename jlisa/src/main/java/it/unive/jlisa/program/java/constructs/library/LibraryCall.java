package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
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
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.List;

/**
 * A method of the Java library whose result is computed by a
 * {@link JavaLibraryFunction} over its arguments, that can be primitive
 * values, strings (whose contents are passed to the function), or instances of
 * wrapper classes (whose wrapped values are passed to the function). If the
 * function returns strings, the result is a new string object holding the
 * computed contents. Subclasses only choose the function and the kinds of the
 * arguments.
 */
public abstract class LibraryCall extends NaryExpression implements PluggableStatement {

	/**
	 * The kind of an argument of a {@link LibraryCall}.
	 */
	public static final class Arg {

		private final String wrapper;

		private final Type wrapped;

		private final boolean string;

		private Arg(
				String wrapper,
				Type wrapped,
				boolean string) {
			this.wrapper = wrapper;
			this.wrapped = wrapped;
			this.string = string;
		}

		/**
		 * A primitive value, passed as it is.
		 */
		public static final Arg VALUE = new Arg(null, null, false);

		/**
		 * A string, whose contents are passed.
		 */
		public static final Arg STRING = new Arg(null, null, true);

		/**
		 * An instance of the given wrapper class, whose wrapped value (of the
		 * given type) is passed.
		 *
		 * @param wrapper the name of the wrapper class
		 * @param wrapped the type of the wrapped values
		 *
		 * @return the kind
		 */
		public static Arg boxed(
				String wrapper,
				Type wrapped) {
			return new Arg(wrapper, wrapped, false);
		}

		private SymbolicExpression read(
				SymbolicExpression expr,
				CodeLocation location) {
			if (!string && wrapper == null)
				return expr;
			GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", location);
			Type container = string ? JavaClassType.getStringType() : JavaClassType.lookup(wrapper);
			Type contents = string ? JavaClassType.getStringType() : wrapped;
			HeapDereference deref = new HeapDereference(container, expr, location);
			return new AccessChild(contents, deref, value, location);
		}
	}

	protected Statement originating;

	private final JavaLibraryFunction function;

	private final Arg[] kinds;

	protected LibraryCall(
			CFG cfg,
			CodeLocation location,
			JavaLibraryFunction function,
			Arg[] kinds,
			Expression... params) {
		super(cfg, location, function.toString(), params);
		if (kinds.length != params.length || kinds.length < 1 || kinds.length > 3)
			throw new IllegalArgumentException("Unsupported arguments for " + function);
		this.function = function;
		this.kinds = kinds;
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemanticsAux(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			ExpressionSet[] params,
			StatementStore<A> expressions)
			throws SemanticException {
		List<List<SymbolicExpression>> combinations = new ArrayList<>();
		combinations.add(new ArrayList<>());
		for (int i = 0; i < params.length; i++) {
			List<List<SymbolicExpression>> extended = new ArrayList<>();
			for (List<SymbolicExpression> prefix : combinations)
				for (SymbolicExpression e : params[i]) {
					List<SymbolicExpression> args = new ArrayList<>(prefix);
					args.add(kinds[i].read(e, getLocation()));
					extended.add(args);
				}
			combinations = extended;
		}

		AnalysisState<A> result = state.bottomExecution();
		for (List<SymbolicExpression> args : combinations) {
			SymbolicExpression call = apply(args);
			result = result.lub(function.returnsString() ? allocate(interprocedural, state, call, expressions)
					: interprocedural.getAnalysis().smallStepSemantics(state, call,
							interprocedural.getCurrentContext(), originating));
		}
		return result;
	}

	private SymbolicExpression apply(
			List<SymbolicExpression> args) {
		Type type = function.getResultType();
		switch (args.size()) {
		case 1:
			return new UnaryExpression(type, args.get(0), function, getLocation());
		case 2:
			return new BinaryExpression(type, args.get(0), args.get(1), function, getLocation());
		default:
			return new TernaryExpression(type, args.get(0), args.get(1), args.get(2), function, getLocation());
		}
	}

	/**
	 * Yields a new string object whose contents are the given value.
	 */
	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> allocate(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression contents,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		JavaNewObj call = new JavaNewObj(getCFG(), (SourceCodeLocation) getLocation(),
				new JavaReferenceType(stringType), new Expression[0]);
		AnalysisState<A> callState = call.forwardSemanticsAux(interprocedural, state,
				new ExpressionSet[0], expressions);
		GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			AccessChild field = new AccessChild(stringType, ref, value, getLocation());
			result = result.lub(analysis.assign(callState, field, contents, context, this));
		}
		getMetaVariables().addAll(call.getMetaVariables());
		return result.withExecutionExpressions(callState.getExecutionExpressions());
	}
}

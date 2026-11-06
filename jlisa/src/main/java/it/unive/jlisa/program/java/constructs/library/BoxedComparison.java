package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * An instance method of a wrapper class of the Java library comparing the
 * receiver with another instance of the same class (e.g.,
 * {@link Integer#compareTo(Integer)}): the result is computed by a
 * {@link JavaLibraryFunction} over the wrapped values. Subclasses only choose
 * the wrapper class, the type of the wrapped values, and the function.
 */
public abstract class BoxedComparison extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	private final String wrapper;

	private final Type wrapped;

	private final JavaLibraryFunction function;

	protected BoxedComparison(
			CFG cfg,
			CodeLocation location,
			String wrapper,
			Type wrapped,
			JavaLibraryFunction function,
			Expression receiver,
			Expression other) {
		super(cfg, location, "compareTo", receiver, other);
		this.wrapper = wrapper;
		this.wrapped = wrapped;
		this.function = function;
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

	private SymbolicExpression unbox(
			SymbolicExpression box) {
		GlobalVariable value = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference deref = new HeapDereference(JavaClassType.lookup(wrapper), box, getLocation());
		return new AccessChild(wrapped, deref, value, getLocation());
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression left,
					SymbolicExpression right,
					StatementStore<A> expressions)
					throws SemanticException {
		it.unive.lisa.symbolic.value.BinaryExpression bin = new it.unive.lisa.symbolic.value.BinaryExpression(
				function.getResultType(),
				unbox(left),
				unbox(right),
				function,
				getLocation());
		return interprocedural.getAnalysis().smallStepSemantics(state, bin, interprocedural.getCurrentContext(),
				originating);
	}
}

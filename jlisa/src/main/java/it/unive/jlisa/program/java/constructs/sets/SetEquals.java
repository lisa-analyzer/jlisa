package it.unive.jlisa.program.java.constructs.sets;

import it.unive.jlisa.program.ConstraintUtils;
import it.unive.jlisa.program.java.constructs.lists.ListModel;
import it.unive.jlisa.program.java.constructs.map.MapModel;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class SetEquals extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public SetEquals(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "equals", left, right);
	}

	public static SetEquals build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SetEquals(cfg, location, params[0], params[1]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		Satisfiability isEmpty = checkZeroSize(interprocedural, state, left, expressions);
		if (isEmpty.isBottom())
			return state.bottomExecution();
		if (isEmpty == Satisfiability.SATISFIED) {

			Satisfiability isEmptyOther = checkZeroSize(interprocedural, state, right, expressions);
			if (isEmptyOther.isBottom())
				return state.bottomExecution();
			if (isEmptyOther == Satisfiability.SATISFIED) {
				Constant c = new Constant(JavaBooleanType.INSTANCE, true, getLocation());
				return analysis.smallStepSemantics(state, c, context, this);
			}
		}

		return topReturnValue(interprocedural, state, expressions);
	}


	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> topReturnValue(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {
		ScopeId<A> context = interprocedural.getCurrentContext();
		AnalysisState<A> topState = interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(JavaBooleanType.INSTANCE, getLocation()),
				context, originating);
		return topState;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Satisfiability checkZeroSize(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {

		CodeLocation location = getLocation();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AccessChild accessSize = ListModel.size(expr, getLocation());

		Constant zero = new Constant(JavaIntType.INSTANCE, 0, location);
		SymbolicExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression(JavaBooleanType.INSTANCE, accessSize,
				zero, ComparisonEq.INSTANCE, location);

		Satisfiability sat = interprocedural.getAnalysis().satisfies(state, eq, context, this);
		return sat;
	}

}


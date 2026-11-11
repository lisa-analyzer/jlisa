package it.unive.jlisa.program.java.constructs.map;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
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
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import java.util.Set;

public class ConcurrentMapGet extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public ConcurrentMapGet(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", left, right);
	}

	public static ConcurrentMapGet build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ConcurrentMapGet(cfg, location, params[0], params[1]);
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
			return analysis.smallStepSemantics(state, new NullConstant(getLocation()), context, this);
		}

		Set<Type> keyTypes = analysis.getRuntimeTypesOf(state, right, context, this);

		AnalysisState<A> res = state.bottomExecution();
		boolean containsNull = keyTypes.remove(new JavaReferenceType(NullType.INSTANCE));

		// throw NullPointerException if key could be null, ConcurrentMap is not
		// null safe
		if (containsNull) {
			res = res.lub(throwNullPointerException(interprocedural, state, expressions));
		}
		// otherwise, as for the other maps
		if (!keyTypes.isEmpty()) {
			MapGet get = new MapGet(getCFG(), getLocation(), getLeft(), getRight());
			// MapGet takes the sub-expressions as its own
			getLeft().setParentStatement(this);
			getRight().setParentStatement(this);
			get.setOriginatingStatement(originating);
			res = res.lub(get.fwdBinarySemantics(interprocedural, state, left, right, expressions));
			getMetaVariables().addAll(get.getMetaVariables());
		}

		return res;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> throwNullPointerException(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			StatementStore<A> expressions)
			throws SemanticException {

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();

		JavaClassType npeExc = JavaClassType.getNullPointerExceptionType();
		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
				npeExc.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);
		AnalysisState<A> exceptionState = state.bottomExecution();

		for (SymbolicExpression th : state.getExecutionExpressions()) {
			// assign exception to variable thrower
			CFGThrow throwVar = new CFGThrow(getCFG(), npeExc.getReference(), getLocation());
			AnalysisState<A> tmp = analysis.assign(state, throwVar, th, context, this);

			// deletes the receiver of the constructor
			// and all the metavariables from subexpressions
			tmp = tmp.forgetIdentifiers(call.getMetaVariables(), this)
					.forgetIdentifiers(getLeft().getMetaVariables(), this)
					.forgetIdentifiers(getRight().getMetaVariables(), this);
			exceptionState = exceptionState.lub(analysis.moveExecutionToError(tmp.withExecutionExpression(throwVar),
					new Error(npeExc.getReference(), originating), context, this));
		}

		return exceptionState;
	}

	private <A extends AbstractLattice<A>, D extends AbstractDomain<A>> Satisfiability checkZeroSize(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression map,
			StatementStore<A> expressions)
			throws SemanticException {

		CodeLocation location = getLocation();
		ScopeId<A> context = interprocedural.getCurrentContext();
		AccessChild accessSize = MapModel.size(map, location);

		Constant zero = new Constant(JavaIntType.INSTANCE, 0, location);
		SymbolicExpression eq = new it.unive.lisa.symbolic.value.BinaryExpression(JavaBooleanType.INSTANCE, accessSize,
				zero, ComparisonEq.INSTANCE, location);

		Satisfiability sat = interprocedural.getAnalysis().satisfies(state, eq, context, this);
		return sat;
	}
}

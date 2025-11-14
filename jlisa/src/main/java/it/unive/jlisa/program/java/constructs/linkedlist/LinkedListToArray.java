package it.unive.jlisa.program.java.constructs.linkedlist;

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
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;

public class LinkedListToArray extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public LinkedListToArray(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "toArray", expr);
	}

	public static LinkedListToArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LinkedListToArray(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		// FIXME: this is a temporary solution: we return an array with length
		// set to top but with
		// one elements, set to the top object
		JavaReferenceType objectArrayType = JavaArrayType.getObjectArray();
		JavaClassType objectType = JavaClassType.getObjectType();
		Analysis<A, D> analysis = interprocedural.getAnalysis();

		String src = ((SourceCodeLocation) getLocation()).getSourceFile();
		Expression unknown = objectArrayType.unknownValue(getCFG(), originating.getLocation());
		AnalysisState<A> array = unknown.forwardSemantics(state, interprocedural, expressions);

		AnalysisState<A> result = state.bottomExecution();
		for (SymbolicExpression spl : array.getExecutionExpressions()) {
			AccessChild ch = new AccessChild(objectType, spl,
					new Constant(JavaIntType.INSTANCE, 0, getLocation()), getLocation());

			// first
			Expression unknownObject = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 1),
					new JavaReferenceType(objectType), new Expression[0]);

			AnalysisState<A> unknownObjectState = unknownObject.forwardSemantics(array, interprocedural, expressions);
			SymbolicExpression topObjectRef = unknownObjectState.getExecutionExpressions().elements().stream().findAny()
					.get();
			result = result.lub(analysis.assign(unknownObjectState, ch,
					topObjectRef, originating));

		}

		return result.withExecutionExpressions(array.getExecutionExpressions());
	}
}

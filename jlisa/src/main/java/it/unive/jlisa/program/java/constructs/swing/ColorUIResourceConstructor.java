package it.unive.jlisa.program.java.constructs.swing;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.libraries.LibrarySpecificationProvider;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.value.PushAny;

public class ColorUIResourceConstructor extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public ColorUIResourceConstructor(
			CFG cfg,
			CodeLocation location,
			Expression thisExpr,
			Expression red,
			Expression green,
			Expression blue) {
		super(cfg, location, "ColorUIResource", thisExpr, red, green, blue);
	}

	public static ColorUIResourceConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ColorUIResourceConstructor(cfg, location, params[0], params[1], params[2], params[3]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		CFG cfg = getCFG();
		CodeLocation location = getLocation();

		AnalysisState<A> noExceptionState;
		AnalysisState<A> exceptionState;

		noExceptionState = analysis.smallStepSemantics(state,
				new PushAny(new JavaReferenceType(JavaClassType.lookup("javax.swing.plaf.ColorUIResource")),
						getLocation()),
				originating);

		LibrarySpecificationProvider.importClass(getProgram(), "java.lang.IllegalArgumentException");
		JavaClassType IllegalArgumentExceptionType = JavaClassType.lookup("java.lang.IllegalArgumentException");

		JavaNewObj call = new JavaNewObj(cfg, location,
				IllegalArgumentExceptionType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		CFGThrow throwVar = new CFGThrow(cfg, IllegalArgumentExceptionType.getReference(), location);
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getSubExpressions()[0].getMetaVariables(), this);
		state = state.forgetIdentifiers(getSubExpressions()[1].getMetaVariables(), this);
		state = state.forgetIdentifiers(getSubExpressions()[2].getMetaVariables(), this);
		state = state.forgetIdentifiers(getSubExpressions()[3].getMetaVariables(), this);

		exceptionState = analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
				new Error(IllegalArgumentExceptionType.getReference(), originating), this);

		return noExceptionState.lub(exceptionState);
	}
}

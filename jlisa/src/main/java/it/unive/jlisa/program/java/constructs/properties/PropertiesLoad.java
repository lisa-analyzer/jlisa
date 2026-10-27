package it.unive.jlisa.program.java.constructs.properties;

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
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.PushAny;

public class PropertiesLoad extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public PropertiesLoad(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "load", left, right);
	}

	public static PropertiesLoad build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new PropertiesLoad(cfg, location, params[0], params[1]);
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
		CFG cfg = getCFG();
		CodeLocation location = getLocation();

		AnalysisState<A> topState = interprocedural.getAnalysis().smallStepSemantics(state,
				new PushAny(new JavaReferenceType(JavaClassType.getStringType()), getLocation()),
				originating);

		LibrarySpecificationProvider.importClass(getProgram(), "java.io.IOException");
		JavaClassType IOExceptionType = JavaClassType.lookup("java.io.IOException");

		JavaNewObj call = new JavaNewObj(cfg, location,
				IOExceptionType.getReference(), new Expression[0]);
		state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

		CFGThrow throwVar = new CFGThrow(cfg, IOExceptionType.getReference(), location);
		state = analysis.assign(state, throwVar,
				state.getExecutionExpressions().elements.stream().findFirst().get(), this);

		state = state.forgetIdentifiers(call.getMetaVariables(), this);
		state = state.forgetIdentifiers(getLeft().getMetaVariables(), this);
		state = state.forgetIdentifiers(getRight().getMetaVariables(), this);

		return analysis.moveExecutionToError(topState, state.withExecutionExpression(throwVar),
				new Error(IOExceptionType.getReference(), originating), this);

	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

}

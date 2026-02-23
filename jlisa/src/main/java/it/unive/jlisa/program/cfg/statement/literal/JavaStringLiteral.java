package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaStringLiteral extends Literal<String> {
	public JavaStringLiteral(
			CFG cfg,
			CodeLocation location,
			String value) {
		super(cfg, location, value, new JavaReferenceType(JavaClassType.lookup("java.lang.String")));
	}

	@Override
	public String toString() {
		return "\"" + getValue() + "\"";
	}

	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
					AnalysisState<A> entryState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Type stringType = getProgram().getTypes().getStringType();
		JavaReferenceType reftype = (JavaReferenceType) new JavaReferenceType(stringType);

		// allocate the string
		JavaNewObj call = new JavaNewObj(getCFG(), getLocation(), reftype, new Expression[0]);
		AnalysisState<
				A> callState = call.forwardSemanticsAux(interprocedural, entryState, new ExpressionSet[0], expressions);

		AnalysisState<A> tmp = entryState.bottomExecution();
		for (SymbolicExpression ref : callState.getExecutionExpressions()) {
			GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
			AccessChild access = new AccessChild(stringType, ref, var, getLocation());
			AnalysisState<A> sem = analysis.assign(callState, access,
					new Constant(stringType, getValue(), getLocation()), this);
			tmp = tmp.lub(sem);
		}

		getMetaVariables().addAll(call.getMetaVariables());
		return tmp.withExecutionExpressions(callState.getExecutionExpressions());
	}
}

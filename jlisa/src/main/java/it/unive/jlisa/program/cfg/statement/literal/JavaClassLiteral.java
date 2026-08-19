package it.unive.jlisa.program.cfg.statement.literal;

import it.unive.jlisa.program.java.constructs.classmetatype.LoadClass;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

public class JavaClassLiteral extends Literal<Type> {

	public JavaClassLiteral(
			CFG cfg,
			CodeLocation location,
			Type value) {
		super(cfg, location, value, new JavaReferenceType(JavaClassType.getClassMetaType()));
	}

	@Override
	public String toString() {
		return "\"" + getValue().toString() + "\"";
	}

	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
					AnalysisState<A> entryState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {

		// TODO: array types, for example int[].class should be valid

		CodeLocation location = getLocation();
		CFG cfg = getCFG();

		Type t = getValue();

		if (t == null)
			return interprocedural.getAnalysis().smallStepSemantics(entryState,
					new PushAny(getStaticType(), getLocation()), this);

		LoadClass loadClass = new LoadClass(t, getValue().toString(), cfg, location);
		AnalysisState<A> callState = loadClass.forwardSemanticsAux(interprocedural, entryState, new ExpressionSet[0],
				expressions);

		return callState;
	}

}

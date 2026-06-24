package it.unive.jlisa.program.java.constructs.classmetatype;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
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

public class FieldGetValue extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public FieldGetValue(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "get", left, right);
	}

	public static FieldGetValue build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FieldGetValue(cfg, location, params[0], params[1]);
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

		Type fieldMetaType = JavaClassType.getFieldMetaType();
		Type objectType = JavaClassType.getObjectType();
		Type stringType = getProgram().getTypes().getStringType();

		GlobalVariable nameVar = new GlobalVariable(Untyped.INSTANCE, "name", getLocation());

		// (*field)->name
		HeapDereference derefField = new HeapDereference(fieldMetaType, left, getLocation());
		AccessChild accessFieldName = new AccessChild(new JavaReferenceType(stringType), derefField, nameVar,
				getLocation());

		// (*target)->[(*field)->name]
		HeapDereference derefTarget = new HeapDereference(objectType, right, getLocation());
		AccessChild accessTargetField = new AccessChild(Untyped.INSTANCE, derefTarget, accessFieldName,
				getLocation());

		return analysis.smallStepSemantics(state, accessTargetField, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

package it.unive.jlisa.program.java.constructs.stringbuilder;

import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.type.JavaIntType;
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
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringBuilderConstructor extends BinaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderConstructor(
			CFG cfg,
			CodeLocation location,
			Expression left,
			Expression right) {
		super(cfg, location, "StringBuilder", left, right);
	}

	public static StringBuilderConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderConstructor(cfg, location, params[0], params[1]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression left,
			SymbolicExpression right,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AccessChild leftAccess = new AccessChild(stringType, left, var, getLocation());
		AccessChild rightAccess = new AccessChild(stringType, right, var, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		AnalysisState<A> tmp = analysis.assign(state, leftAccess, rightAccess, originating);

		// capacity of a StringBuilder(String) is initialValue.length() + 16,
		// as per the Java spec
		GlobalVariable capVar = new GlobalVariable(Untyped.INSTANCE, "capacity", getLocation());
		AccessChild capAccess = new AccessChild(JavaIntType.INSTANCE, left, capVar, getLocation());
		it.unive.lisa.symbolic.value.UnaryExpression rightLength = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE, rightAccess, JavaStringLengthOperator.INSTANCE, getLocation());
		it.unive.lisa.symbolic.value.BinaryExpression capacityValue = new it.unive.lisa.symbolic.value.BinaryExpression(
				JavaIntType.INSTANCE, rightLength, new Constant(JavaIntType.INSTANCE, 16, getLocation()),
				NumericNonOverflowingAdd.INSTANCE, getLocation());

		return analysis.assign(tmp, capAccess, capacityValue, originating);
	}
}

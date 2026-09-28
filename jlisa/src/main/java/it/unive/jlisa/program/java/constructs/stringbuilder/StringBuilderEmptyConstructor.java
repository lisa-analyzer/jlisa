package it.unive.jlisa.program.java.constructs.stringbuilder;

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
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringBuilderEmptyConstructor extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public StringBuilderEmptyConstructor(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "StringBuilder", exp);
	}

	public static StringBuilderEmptyConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringBuilderEmptyConstructor(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Type stringType = getProgram().getTypes().getStringType();
		Constant emptyString = new Constant(stringType, "", getLocation());

		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		AccessChild access = new AccessChild(stringType, expr, var, getLocation());

		GlobalVariable capVar = new GlobalVariable(Untyped.INSTANCE, "capacity", getLocation());
		AccessChild capAccess = new AccessChild(JavaIntType.INSTANCE, expr, capVar, getLocation());
		// default capacity of a no-arg StringBuilder, as per the Java spec
		Constant defaultCapacity = new Constant(JavaIntType.INSTANCE, 16, getLocation());

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		AnalysisState<A> tmp = analysis.assign(state, access, emptyString, originating);
		return analysis.assign(tmp, capAccess, defaultCapacity, originating);
	}
}

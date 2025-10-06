package it.unive.jlisa.program.java.constructs.doublew;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.VoidType;

public class DoubleClassInitializer extends NativeCFG implements PluggableStatement {

	protected Statement originating;

	public DoubleClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {

		super(new JavaCodeMemberDescriptor(location, objectUnit, false,
				"DoubleClInit" + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Parameter[0]),
				DoubleClassInitializer.DoubleClInit.class);
	}

	public static DoubleClassInitializer.DoubleClInit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new DoubleClassInitializer.DoubleClInit(cfg, location);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	public static class DoubleClInit extends NaryExpression implements PluggableStatement {
		protected Statement originating;

		public DoubleClInit(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location, "Double" + InitializedClassSet.SUFFIX_CLINIT, JavaClassType.getSystemType());
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
			GlobalVariable maxId = new GlobalVariable(JavaDoubleType.INSTANCE, "java.lang.DoubleClInit::MAX_VALUE",
					getLocation());
			Constant maxConst = new Constant(JavaDoubleType.INSTANCE, Double.MAX_VALUE, getLocation());

			GlobalVariable minId = new GlobalVariable(JavaDoubleType.INSTANCE, "java.lang.DoubleClInit::MIN_VALUE",
					getLocation());
			Constant minConst = new Constant(JavaDoubleType.INSTANCE, Double.MIN_VALUE, getLocation());

			GlobalVariable posInfId = new GlobalVariable(JavaDoubleType.INSTANCE,
					"java.lang.DoubleClInit::POSITIVE_INFINITY",
					getLocation());
			Constant posInfConst = new Constant(JavaDoubleType.INSTANCE, Double.POSITIVE_INFINITY, getLocation());

			Analysis<A, D> analysis = interprocedural.getAnalysis();
			state = analysis.assign(state, maxId, maxConst, this);
			state = analysis.assign(state, minId, minConst, this);
			state = analysis.assign(state, posInfId, posInfConst, this);
			return state;
		}
	}
}

package it.unive.jlisa.program.java.constructs.floatw;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaFloatType;
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

public class FloatClassInitializer extends NativeCFG implements PluggableStatement {

	protected Statement originating;

	public FloatClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {

		super(new JavaCodeMemberDescriptor(location, objectUnit, false,
				"FloatClInit" + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Parameter[0]),
				FloatClassInitializer.FloatClInit.class);
	}

	public static FloatClassInitializer.FloatClInit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new FloatClassInitializer.FloatClInit(cfg, location);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	public static class FloatClInit extends NaryExpression implements PluggableStatement {
		protected Statement originating;

		public FloatClInit(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location, "Float" + InitializedClassSet.SUFFIX_CLINIT, JavaClassType.getSystemType());
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
			GlobalVariable maxId = new GlobalVariable(JavaFloatType.INSTANCE, "java.lang.FloatClInit::MAX_VALUE",
					getLocation());
			Constant maxConst = new Constant(JavaFloatType.INSTANCE, Float.MAX_VALUE, getLocation());

			GlobalVariable minId = new GlobalVariable(JavaFloatType.INSTANCE, "java.lang.FloatClInit::MIN_VALUE",
					getLocation());
			Constant minConst = new Constant(JavaFloatType.INSTANCE, Float.MIN_VALUE, getLocation());

			GlobalVariable posInfId = new GlobalVariable(JavaFloatType.INSTANCE,
					"java.lang.FloatClInit::POSITIVE_INFINITY",
					getLocation());
			Constant posInfConst = new Constant(JavaFloatType.INSTANCE, Float.POSITIVE_INFINITY, getLocation());

			Analysis<A, D> analysis = interprocedural.getAnalysis();
			state = analysis.assign(state, maxId, maxConst, this);
			state = analysis.assign(state, minId, minConst, this);
			state = analysis.assign(state, posInfId, posInfConst, this);
			return state;
		}
	}
}

package it.unive.jlisa.program.java.constructs.shor;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.type.JavaShortType;
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

public class ShortClassInitializer extends NativeCFG implements PluggableStatement {

	protected Statement originating;

	public ShortClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {

		super(new JavaCodeMemberDescriptor(location, objectUnit, false, "Short" + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Parameter[0]),
				ShortClassInitializer.ShortClInit.class);
	}

	public static ShortClassInitializer.ShortClInit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ShortClassInitializer.ShortClInit(cfg, location);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	public static class ShortClInit extends NaryExpression implements PluggableStatement {
		protected Statement originating;

		public ShortClInit(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location, "Short" + InitializedClassSet.SUFFIX_CLINIT, VoidType.INSTANCE);
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
			GlobalVariable maxId = new GlobalVariable(JavaShortType.INSTANCE, "java.lang.Short::MAX_VALUE",
					getLocation());
			GlobalVariable minId = new GlobalVariable(JavaShortType.INSTANCE, "java.lang.Short::MIN_VALUE",
					getLocation());
			Constant maxConst = new Constant(JavaShortType.INSTANCE, 0x7fff, getLocation());
			Constant minConst = new Constant(JavaShortType.INSTANCE, 0x8000, getLocation());
			Analysis<A, D> analysis = interprocedural.getAnalysis();
			state = analysis.assign(state, maxId, maxConst, this);
			state = analysis.assign(state, minId, minConst, this);
			return state;
		}
	}
}

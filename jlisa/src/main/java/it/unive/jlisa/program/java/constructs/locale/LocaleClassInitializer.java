package it.unive.jlisa.program.java.constructs.locale;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.SyntheticCodeLocation;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.VoidType;

public class LocaleClassInitializer extends NativeCFG implements PluggableStatement {

	protected Statement originating;

	public LocaleClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {

		super(new JavaCodeMemberDescriptor(location, objectUnit, false,
				"Locale" + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Parameter[0]),
				LocaleClassInitializer.FloatClInit.class);
	}

	public static LocaleClassInitializer.FloatClInit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new LocaleClassInitializer.FloatClInit(cfg, location);
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
			super(cfg, location, "Locale" + InitializedClassSet.SUFFIX_CLINIT, VoidType.INSTANCE);
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
			JavaClassType localeType = JavaClassType.getLocaleType();
			String src = ((SourceCodeLocation) getLocation()).getSourceFile() + "::Locale";

			GlobalVariable uk = new GlobalVariable(localeType.getReference(), "java.util.Locale::UK",
					getLocation());
			Expression unknownLocaleForUk = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 1), localeType.getReference(), new Expression[0]);
			
			GlobalVariable eng = new GlobalVariable(localeType.getReference(), "java.util.Locale::ENGLISH",
					getLocation());
			Expression unknownLocaleForEnglish = new JavaNewObj(getCFG(), new SyntheticCodeLocation(src, 2), localeType.getReference(), new Expression[0]);


			AnalysisState<A> ukState = unknownLocaleForUk.forwardSemantics(state, interprocedural, expressions);
			Analysis<A, D> analysis = interprocedural.getAnalysis();

			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression ukVal : ukState.getExecutionExpressions())
				tmp = tmp.lub(analysis.assign(ukState, uk, ukVal, originating));
			
			AnalysisState<A> engState = unknownLocaleForEnglish.forwardSemantics(tmp, interprocedural, expressions);

			tmp = state.bottomExecution();
			for (SymbolicExpression engVal : engState.getExecutionExpressions())
				tmp = tmp.lub(analysis.assign(engState, eng, engVal, originating));
			
			return tmp;
		}
	}
}

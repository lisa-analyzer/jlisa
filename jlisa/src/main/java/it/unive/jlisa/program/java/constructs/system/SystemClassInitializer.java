package it.unive.jlisa.program.java.constructs.system;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.cfg.statement.global.JavaAccessGlobal;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.VoidType;

public class SystemClassInitializer extends NativeCFG implements PluggableStatement {
//	private static final String GLOBAL_OUT = "out";
//	private static final String GLOBAL_OUT_CLASS_NAME = "PrintStream";

	protected Statement originating;

	public SystemClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {

		super(new JavaCodeMemberDescriptor(location, objectUnit, false, "System" + InitializedClassSet.SUFFIX_CLINIT,
				VoidType.INSTANCE,
				new Parameter[0]),
				SystemClassInitializer.SystemClInit.class);
	}

	public static SystemClassInitializer.SystemClInit build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new SystemClassInitializer.SystemClInit(cfg, location);
	}

	@Override
	public void setOriginatingStatement(
			Statement st) {
		originating = st;
	}

	public static class SystemClInit extends NaryExpression implements PluggableStatement {
		protected Statement originating;

		public SystemClInit(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location, "System" + InitializedClassSet.SUFFIX_CLINIT, JavaClassType.getSystemType());
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
			JavaClassType printWriterClassType = JavaClassType.getPrintStreamType();
			JavaClassType systemType = JavaClassType.getSystemType();
			JavaAccessGlobal accessGlobal = new JavaAccessGlobal(
					getCFG(),
					getLocation(),
					systemType.getUnit(),
					new Global(
							getLocation(),
							getUnit(),
							"out",
							false,
							printWriterClassType));
			JavaNewObj newOut = new JavaNewObj(
					getCFG(),
					getLocation(),
					new JavaReferenceType(printWriterClassType));
			AnalysisState<A> callState = newOut.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					expressions);
			AnalysisState<A> accessGlobalState = accessGlobal.forwardSemantics(state, interprocedural, expressions);
			AnalysisState<A> tmp = state.bottomExecution();
			for (SymbolicExpression callExpr : callState.getExecutionExpressions()) {
				for (SymbolicExpression accessGlobalExpr : accessGlobalState.getExecutionExpressions())
					tmp = tmp.lub(interprocedural.getAnalysis().assign(callState, accessGlobalExpr, callExpr, this));
			}

			return tmp.withExecutionExpressions(state.getExecutionExpressions());
		}
	}
}

package it.unive.jlisa.program.java.constructs.system;

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
	private static final String CODE_MEMBER_DESCRIPTOR = "System_clinit";
	private static final String GLOBAL_OUT = "out";
	private static final String GLOBAL_OUT_CLASS_NAME = "PrintStream";

	protected Statement originating;

	public SystemClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {

		super(new JavaCodeMemberDescriptor(location, objectUnit, false, CODE_MEMBER_DESCRIPTOR, VoidType.INSTANCE,
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
			super(cfg, location, CODE_MEMBER_DESCRIPTOR, JavaClassType.lookup("System", null));
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
			JavaClassType printWriterClassType = JavaClassType.lookup(GLOBAL_OUT_CLASS_NAME, null);
			JavaAccessGlobal accessGlobal = new JavaAccessGlobal(
					getCFG(),
					getLocation(),
					getUnit(),
					new Global(
							getLocation(),
							getUnit(),
							GLOBAL_OUT,
							false,
							printWriterClassType));
			JavaNewObj newOut = new JavaNewObj(
					getCFG(),
					getLocation(),
					printWriterClassType.getUnit().getName(),
					new JavaReferenceType(printWriterClassType));
			AnalysisState<A> callState = newOut.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					expressions);
			AnalysisState<A> accessGlobalState = accessGlobal.forwardSemantics(state, interprocedural, expressions);
			AnalysisState<A> tmp = state.bottom();
			for (SymbolicExpression callExpr : callState.getExecutionExpressions()) {
				for (SymbolicExpression accessGlobalExpr : accessGlobalState.getExecutionExpressions()) {
					AnalysisState<
							A> sem = interprocedural.getAnalysis().assign(callState, accessGlobalExpr, callExpr, this);
					tmp = tmp.lub(sem);
				}
			}

			// getMetaVariables().addAll(call.getMetaVariables());
			return tmp.withExecutionExpressions(state.getExecutionExpressions());
		}
	}
}

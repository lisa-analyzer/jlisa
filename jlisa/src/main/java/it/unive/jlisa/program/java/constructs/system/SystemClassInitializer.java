package it.unive.jlisa.program.java.constructs.system;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.jlisa.program.cfg.SyntheticCodeLocation;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
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

public class SystemClassInitializer extends NativeCFG implements PluggableStatement {
	// private static final String GLOBAL_OUT = "out";
	// private static final String GLOBAL_OUT_CLASS_NAME = "PrintStream";

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
			JavaClassType inputStreamClassType = JavaClassType.getInputStreamType();
			JavaReferenceType refPrinterType = new JavaReferenceType(printWriterClassType);
			JavaReferenceType refInputStreamType = new JavaReferenceType(inputStreamClassType);

			String src = ((SourceCodeLocation) getLocation()).getSourceFile() + "::System";
			Analysis<A, D> analysis = interprocedural.getAnalysis();

			// System.out
			SyntheticCodeLocation outLocation = new SyntheticCodeLocation(src, 1);
			JavaNewObj newOut = new JavaNewObj(
					getCFG(),
					outLocation,
					refPrinterType);
			AnalysisState<A> outCallState = newOut.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0],
					expressions);
			AnalysisState<A> tmpOut = state.bottomExecution();
			for (SymbolicExpression callExpr : outCallState.getExecutionExpressions()) {
				GlobalVariable outId = new GlobalVariable(refPrinterType, "java.lang.System::out",
						outLocation);
				tmpOut = tmpOut.lub(analysis.assign(outCallState, outId, callExpr, originating));
			}

			// System.err
			SyntheticCodeLocation errLocation = new SyntheticCodeLocation(src, 2);
			JavaNewObj newErr = new JavaNewObj(
					getCFG(),
					errLocation,
					refPrinterType);
			AnalysisState<A> errCallState = newErr.forwardSemanticsAux(interprocedural, tmpOut, new ExpressionSet[0],
					expressions);

			AnalysisState<A> tmpErr = state.bottomExecution();
			for (SymbolicExpression callExpr : errCallState.getExecutionExpressions()) {
				GlobalVariable errId = new GlobalVariable(refPrinterType, "java.lang.System::err",
						errLocation);
				tmpErr = tmpErr.lub(analysis.assign(errCallState, errId, callExpr, originating));
			}

			// System.in
			SyntheticCodeLocation inLocation = new SyntheticCodeLocation(src, 3);
			JavaNewObj newIn = new JavaNewObj(
					getCFG(),
					inLocation,
					refInputStreamType);
			AnalysisState<A> inCallState = newIn.forwardSemanticsAux(interprocedural, tmpErr, new ExpressionSet[0],
					expressions);

			AnalysisState<A> tmpIn = state.bottomExecution();
			for (SymbolicExpression callExpr : inCallState.getExecutionExpressions()) {
				GlobalVariable inId = new GlobalVariable(refInputStreamType, "java.lang.System::in",
						inLocation);
				tmpIn = tmpIn.lub(analysis.assign(inCallState, inId, callExpr, originating));
			}

			tmpIn = tmpIn.forgetIdentifiers(newIn.getMetaVariables(), this)
					.forgetIdentifiers(newErr.getMetaVariables(), this)
					.forgetIdentifiers(newOut.getMetaVariables(), this);

			return tmpIn;
		}
	}
}

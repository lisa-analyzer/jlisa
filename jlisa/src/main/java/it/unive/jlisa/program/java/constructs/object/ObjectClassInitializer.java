package it.unive.jlisa.program.java.constructs.object;

import it.unive.jlisa.program.cfg.JavaCodeMemberDescriptor;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
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
import it.unive.lisa.type.VoidType;

public class ObjectClassInitializer extends NativeCFG {

	public ObjectClassInitializer(
			CodeLocation location,
			ClassUnit objectUnit) {
		super(new JavaCodeMemberDescriptor(location, objectUnit, false, "Object_clinit", VoidType.INSTANCE,
				new Parameter[0]),
				ObjectClassInitializer.ObjectClassInitiliazer.class);
	}

	public static class ObjectClassInitiliazer extends NaryExpression implements PluggableStatement {
		protected Statement originating;

		public ObjectClassInitiliazer(
				CFG cfg,
				CodeLocation location) {
			super(cfg, location, "Object_clinit");
		}

		public static ObjectClassInitializer.ObjectClassInitiliazer build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ObjectClassInitializer.ObjectClassInitiliazer(cfg, location);
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
			return state; // do nothing.
		}
	}
}

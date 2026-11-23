package it.unive.jlisa.program.java.constructs.swing;

import it.unive.jlisa.program.cfg.JavaExceptions;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NaryExpression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;

public class ColorUIResourceConstructor extends NaryExpression implements PluggableStatement {
	protected Statement originating;

	public ColorUIResourceConstructor(
			CFG cfg,
			CodeLocation location,
			Expression thisExpr,
			Expression red,
			Expression green,
			Expression blue) {
		super(cfg, location, "ColorUIResource", thisExpr, red, green, blue);
	}

	public static ColorUIResourceConstructor build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ColorUIResourceConstructor(cfg, location, params[0], params[1], params[2], params[3]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();

		// the components (after the receiver) must be between 0 and 255,
		// otherwise IllegalArgumentException is raised
		JavaExceptions.Raising<A, D> raising = JavaExceptions.Raising.ofNative(interprocedural, this, originating,
				getSubExpressions());
		String iae = "java.lang.IllegalArgumentException";
		AnalysisState<A> valid = state;
		for (int i = 1; i < params.length; i++)
			if (params[i].size() != 1) {
				// the values of the component are not known one by one
				raising.raise(Satisfiability.UNKNOWN, valid, iae);
				continue;
			} else {
				SymbolicExpression c = params[i].iterator().next();
				BinaryExpression outOfRange = new BinaryExpression(JavaBooleanType.INSTANCE,
						new BinaryExpression(JavaBooleanType.INSTANCE, c,
								new Constant(JavaIntType.INSTANCE, 0, location), ComparisonLt.INSTANCE, location),
						new BinaryExpression(JavaBooleanType.INSTANCE, c,
								new Constant(JavaIntType.INSTANCE, 255, location), ComparisonGt.INSTANCE, location),
						LogicalOr.INSTANCE, location);
				valid = raising.raiseWhere(outOfRange, true, valid, iae);
			}

		AnalysisState<A> noExceptionState = valid.getExecutionState().isBottom() ? valid
				: analysis.smallStepSemantics(valid,
						new PushAny(new JavaReferenceType(JavaClassType.lookup("javax.swing.plaf.ColorUIResource")),
								location),
						context, originating);
		return raising.moveFrom(noExceptionState);
	}
}

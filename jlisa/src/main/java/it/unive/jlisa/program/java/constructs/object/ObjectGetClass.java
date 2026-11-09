package it.unive.jlisa.program.java.constructs.object;

import it.unive.jlisa.frontend.InitializedClassSet;
import it.unive.jlisa.program.java.constructs.classmetatype.LoadClass;
import it.unive.jlisa.program.type.JavaClassType;
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
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import java.util.Set;

public class ObjectGetClass extends it.unive.lisa.program.cfg.statement.UnaryExpression implements PluggableStatement {
	protected Statement originating;

	public ObjectGetClass(
			CFG cfg,
			CodeLocation location,
			Expression expr) {
		super(cfg, location, "getClass", JavaClassType.getClassMetaType(), expr);
	}

	public static ObjectGetClass build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ObjectGetClass(cfg, location, params[0]);
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

		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		CodeLocation location = getLocation();
		CFG cfg = getCFG();
		TypeSystem typeSystem = getProgram().getTypes();

		Set<Type> types = analysis.getRuntimeTypesOf(state, expr, context, this);

		AnalysisState<A> tmp = state;
		ExpressionSet execExpressions = new ExpressionSet();

		for (Type t : types) {

			if (t instanceof JavaReferenceType jrt) {
				t = jrt.getInnerType();
			}

			String clazzName = t.toString();
			UnitType ut = getTypeFromStr(clazzName);

			// NOTE: should never happen
			if (ut == null)
				continue;

			// static initializer
			CompilationUnit cu = ut.getUnit();
			tmp = InitializedClassSet.initialize(tmp, new JavaReferenceType(ut), this, interprocedural);

			for (CompilationUnit ancestorCu : cu.getImmediateAncestors().stream().toList()) {

				if (ancestorCu instanceof InterfaceUnit) {
					// TODO: only call the static initializer iff the
					// interface contains a default method
					continue;
				}

				Type ancestorType = typeSystem.getType(ancestorCu.getName());
				assert (ancestorType instanceof UnitType);
				tmp = InitializedClassSet.initialize(tmp, new JavaReferenceType(ancestorType), this,
						interprocedural);
			}

			LoadClass loadClass = new LoadClass(ut, clazzName, cfg, location);
			AnalysisState<A> callState = loadClass.forwardSemanticsAux(interprocedural, tmp, new ExpressionSet[0],
					expressions);

			ExpressionSet clazz = callState.getExecutionExpressions();
			execExpressions = execExpressions.lub(clazz);

			tmp = callState;
		}

		return tmp.withExecutionExpressions(execExpressions);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}

	private UnitType getTypeFromStr(
			String clazzName) {

		clazzName = clazzName.replace('$', '.');

		Type t = getProgram().getTypes().getType(clazzName);

		if (!(t instanceof UnitType))
			return null;

		return (UnitType) t;
	}

}

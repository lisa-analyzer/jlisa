package it.unive.jlisa.program.java.constructs.map;

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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;

/**
 * The methods {@code getKey()} and {@code getValue()} of {@code Map.Entry}:
 * they read the fields {@code $key} and {@code $value} of the entries of
 * {@link MapEntrySet}; the other entries behave as calls to methods that are
 * not modeled.
 */
public abstract class MapEntryGet extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	private final String field;

	protected MapEntryGet(
			CFG cfg,
			CodeLocation location,
			String name,
			String field,
			Expression expr) {
		super(cfg, location, name, expr);
		this.field = field;
	}

	/**
	 * Returns the access to the given field of the given entry.
	 *
	 * @param entry    the entry
	 * @param name     the name of the field
	 * @param location the location of the access
	 *
	 * @return the access to the field
	 */
	public static AccessChild field(
			SymbolicExpression entry,
			String name,
			CodeLocation location) {
		return new AccessChild(new JavaReferenceType(JavaClassType.getObjectType()),
				new HeapDereference(JavaClassType.lookup("java.util.Map.Entry"), entry, location),
				new GlobalVariable(Untyped.INSTANCE, name, location), location);
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
		AccessChild access = field(expr, field, getLocation());
		boolean known = false;
		for (SymbolicExpression e : analysis.rewrite(state, access, context, this))
			if (e instanceof Identifier id && state.knowsIdentifier(id))
				known = true;
		if (!known)
			return analysis.smallStepSemantics(state,
					new PushAny(new JavaReferenceType(JavaClassType.getObjectType()), getLocation()), context, this);
		return analysis.smallStepSemantics(state, access, context, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

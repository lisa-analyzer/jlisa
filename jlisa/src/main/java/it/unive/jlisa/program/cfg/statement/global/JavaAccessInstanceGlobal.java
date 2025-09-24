package it.unive.jlisa.program.cfg.statement.global;

import java.util.HashSet;
import java.util.Set;

import it.unive.jlisa.frontend.exceptions.ParsingException;
import it.unive.jlisa.program.cfg.expression.JavaNewObj;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Assignment;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.program.language.hierarchytraversal.HierarchyTraversalStrategy;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class JavaAccessInstanceGlobal extends UnaryExpression {

	private final String target;

	public JavaAccessInstanceGlobal(
			CFG cfg,
			CodeLocation location,
			Expression receiver,
			String target) {
		super(cfg, location, "::", getType(receiver, target, location), receiver);
		this.target = target;
		receiver.setParentStatement(this);
	}

	private static Type getType(
			Expression receiver,
			String target,
			CodeLocation location) {
		Type recType = receiver.getStaticType();
		if (recType.isReferenceType())
			recType = recType.asReferenceType().getInnerType();
		if (recType.isUnitType()) {
			CompilationUnit unit = recType.asUnitType().getUnit();
			Global global = unit.getInstanceGlobal(target, true);
			if (global != null)
				return global.getStaticType();
		} else if (recType.isArrayType() && target.equals("length"))
			// the only instance global of an array type is "length"
			return JavaIntType.INSTANCE;

		throw new ParsingException(
			"missing-global",
			ParsingException.Type.UNSUPPORTED_STATEMENT,
			"Cannot access instance global " + target + " from an expression of type " + receiver.getStaticType(),
			location);
	}

	/**
	 * Yields the expression that determines the receiver of the global access
	 * defined by this expression.
	 *
	 * @return the receiver of the access
	 */
	public Expression getReceiver() {
		return getSubExpression();
	}

	/**
	 * Yields the instance {@link Global} targeted by this expression.
	 *
	 * @return the global
	 */
	public String getTarget() {
		return target;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaAccessInstanceGlobal other = (JavaAccessInstanceGlobal) obj;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		JavaAccessInstanceGlobal other = (JavaAccessInstanceGlobal) o;
		return target.compareTo(other.target);
	}

	@Override
	public String toString() {
		return getSubExpression() + "::" + target;
	}

	@Override
	public <A extends AbstractLattice<A>,
			D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
					InterproceduralAnalysis<A, D> interprocedural,
					AnalysisState<A> state,
					SymbolicExpression expr,
					StatementStore<A> expressions)
					throws SemanticException {
		CodeLocation loc = getLocation();

		AnalysisState<A> result = state.bottomExecution();
		boolean atLeastOne = false;
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		Set<Type> types = analysis.getRuntimeTypesOf(state, expr, this);

		for (Type recType : types)
			if (recType.isPointerType()) {
				Type inner = recType.asPointerType().getInnerType();
				if (inner.isNullType()) {
					// builds the exception
					JavaClassType npeType = JavaClassType.getNullPointerExceptionType();
					JavaNewObj call = new JavaNewObj(getCFG(), getLocation(),
							npeType.getReference(), new Expression[0]);
					state = call.forwardSemanticsAux(interprocedural, state, new ExpressionSet[0], expressions);

					// assign exception to variable thrower
					CFGThrow throwVar = new CFGThrow(getCFG(), npeType.getReference(), getLocation());
					state = analysis.assign(state, throwVar,
							state.getExecutionExpressions().elements.stream().findFirst().get(), this);

					// deletes the receiver of the constructor
					// and all the metavariables from subexpressions
					state = state.forgetIdentifiers(call.getMetaVariables(), this);
					state = state.forgetIdentifiers(getSubExpression().getMetaVariables(), this);
					result = result.lub(analysis.moveExecutionToError(state.withExecutionExpression(throwVar),
							new Error(npeType.getReference(), this)));
					atLeastOne = true;
					continue;
				} else if (!inner.isUnitType())
					continue;

				HeapDereference container = new HeapDereference(inner, expr, loc);
				CompilationUnit unit = inner.asUnitType().getUnit();

				Set<CompilationUnit> seen = new HashSet<>();
				HierarchyTraversalStrategy strategy = getProgram().getFeatures().getTraversalStrategy();
				for (CompilationUnit cu : strategy.traverse(this, unit))
					if (seen.add(unit)) {
						Global global = cu.getInstanceGlobal(target, false);
						if (global != null) {
							GlobalVariable var = global.toSymbolicVariable(loc);
							AccessChild access = new AccessChild(global.getStaticType(), container, var, loc);
							if (getParentStatement() instanceof Assignment) {
								Assignment asg = (Assignment) getParentStatement();
								if (asg.getLeft().equals(this))
									result = result.lub(analysis.smallStepSemantics(state, access, this));
								else
									result = result.lub(analysis.smallStepSemantics(state,
											new HeapReference(global.getStaticType(), access, loc), this));
							} else if (global.getStaticType().isPointerType())
								result = result.lub(analysis.smallStepSemantics(state, access, this));
							else
								result = result.lub(analysis.smallStepSemantics(state, access, this));
							atLeastOne = true;
						}
					}
			}

		if (atLeastOne)
			return result;

		// worst case: we are accessing a global that we know nothing about
		Set<Type> rectypes = new HashSet<>();
		for (Type t : types)
			if (t.isPointerType())
				rectypes.add(t.asPointerType().getInnerType());

		if (rectypes.isEmpty())
			return state.bottomExecution();

		Type rectype = Type.commonSupertype(rectypes, Untyped.INSTANCE);
		Variable var = new Variable(Untyped.INSTANCE, target, new Annotations(), getLocation());
		HeapDereference container = new HeapDereference(rectype, expr, getLocation());
		AccessChild access = new AccessChild(Untyped.INSTANCE, container, var, getLocation());
		return analysis.smallStepSemantics(state, access, this);
	}
}

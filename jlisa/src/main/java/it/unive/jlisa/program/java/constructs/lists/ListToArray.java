package it.unive.jlisa.program.java.constructs.lists;

import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import java.util.Set;

public class ListToArray
		extends
		it.unive.lisa.program.cfg.statement.UnaryExpression
		implements
		PluggableStatement {
	protected Statement originating;

	public ListToArray(
			CFG cfg,
			CodeLocation location,
			Expression arg) {
		super(cfg, location, "toArray", arg);
	}

	public static ListToArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new ListToArray(cfg, location, params[0]);
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
		JavaReferenceType refType = JavaArrayType.OBJECT_ARRAY;
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());

		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, this);
		AnalysisState<A> tmp = analysis.assign(allocated, array, ref, this);
		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "length", getLocation());
		AccessChild lenAccess = new AccessChild(refType.getInnerType(), array, lenProperty, getLocation());
		PushFromConstraints length = new PushFromConstraints(JavaIntType.INSTANCE, getLocation(), Set.of(
				new BinaryExpression(
						JavaBooleanType.INSTANCE,
						new Constant(JavaIntType.INSTANCE, 0, getLocation()),
						lenAccess,
						ComparisonLe.INSTANCE,
						getLocation())));
		tmp = analysis.assign(tmp, lenAccess, length, this);

		getMetaVariables().add(array);
		return analysis.smallStepSemantics(tmp, array, this);
	}

	@Override
	protected int compareSameClassAndParams(
			Statement o) {
		return 0;
	}
}

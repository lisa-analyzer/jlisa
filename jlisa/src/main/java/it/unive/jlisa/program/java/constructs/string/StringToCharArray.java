package it.unive.jlisa.program.java.constructs.string;

import it.unive.jlisa.program.java.constructs.CharArrayConstantSupport;
import it.unive.jlisa.program.operator.JavaStringCharAtOperator;
import it.unive.jlisa.program.operator.JavaStringLengthOperator;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaCharType;
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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.PluggableStatement;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnaryExpression;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class StringToCharArray extends UnaryExpression implements PluggableStatement {
	protected Statement originating;

	// the maximum length whose characters are copied into the cells
	private static final int MAX_COPIED_CELLS = 64;

	public StringToCharArray(
			CFG cfg,
			CodeLocation location,
			Expression exp) {
		super(cfg, location, "toCharArray", JavaArrayType.CHAR_ARRAY, exp);
	}

	public static StringToCharArray build(
			CFG cfg,
			CodeLocation location,
			Expression... params) {
		return new StringToCharArray(cfg, location, params[0]);
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
	public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdUnarySemantics(
			InterproceduralAnalysis<A, D> interprocedural,
			AnalysisState<A> state,
			SymbolicExpression expr,
			StatementStore<A> expressions)
			throws SemanticException {
		Analysis<A, D> analysis = interprocedural.getAnalysis();
		ScopeId<A> context = interprocedural.getCurrentContext();
		Type stringType = getProgram().getTypes().getStringType();
		GlobalVariable var = new GlobalVariable(Untyped.INSTANCE, "value", getLocation());
		HeapDereference deref = new HeapDereference(stringType, expr, getLocation());
		AccessChild value = new AccessChild(stringType, deref, var, getLocation());
		it.unive.lisa.symbolic.value.UnaryExpression length = new it.unive.lisa.symbolic.value.UnaryExpression(
				JavaIntType.INSTANCE, value, JavaStringLengthOperator.INSTANCE, getLocation());

		// a new array, as for new char[s.length()]
		JavaReferenceType refType = JavaArrayType.CHAR_ARRAY;
		MemoryAllocation created = new MemoryAllocation(refType.getInnerType(), getLocation(), false);
		HeapReference ref = new HeapReference(refType, created, getLocation());
		AnalysisState<A> allocated = analysis.smallStepSemantics(state, created, context, this);
		InstrumentedReceiver array = new InstrumentedReceiver(refType, true, getLocation());
		AnalysisState<A> result = analysis.assign(allocated, array, ref, context, this);
		Variable lenProperty = new Variable(JavaIntType.INSTANCE, "length", getLocation());
		AccessChild lenAccess = new AccessChild(refType.getInnerType(), array, lenProperty, getLocation());
		result = analysis.assign(result, lenAccess, length, context, this);

		// the cells hold the characters of the string when its length is
		// known (otherwise they are not tracked)
		Object known = CharArrayConstantSupport.extractConstantValue(interprocedural, state, length, this);
		if (known instanceof Integer n && n >= 0 && n <= MAX_COPIED_CELLS)
			for (int i = 0; i < n; i++) {
				Constant idx = new Constant(JavaIntType.INSTANCE, i, getLocation());
				AccessChild cell = new AccessChild(JavaCharType.INSTANCE, array, idx, getLocation());
				BinaryExpression ch = new BinaryExpression(JavaCharType.INSTANCE, value, idx,
						JavaStringCharAtOperator.INSTANCE, getLocation());
				result = analysis.assign(result, cell, ch, context, this);
			}

		getMetaVariables().add(array);
		return analysis.smallStepSemantics(result, array, context, originating);
	}
}

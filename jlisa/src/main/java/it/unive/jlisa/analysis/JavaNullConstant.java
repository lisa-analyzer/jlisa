package it.unive.jlisa.analysis;

import it.unive.jlisa.program.type.JavaNullType;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;

public class JavaNullConstant extends HeapExpression {
	
	public JavaNullConstant(CodeLocation location) {
		super(JavaNullType.INSTANCE, location);
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor, Object... params) throws SemanticException {
		return visitor.visit(this, null, params);
	}

	@Override
	public String toString() {
		return "null";
	}
	
	@Override
	public SymbolicExpression removeTypingExpressions() {
		return this;
	}

	@Override
	public SymbolicExpression replace(SymbolicExpression source, SymbolicExpression target) {
		if (this.equals(source))
			return target;
		return this;
	}
}

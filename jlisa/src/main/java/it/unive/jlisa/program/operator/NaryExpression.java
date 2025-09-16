package it.unive.jlisa.program.operator;

import java.lang.reflect.Array;
import java.util.List;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.lattices.heap.allocations.HeapAllocationSite;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.type.Type;

public class NaryExpression extends ValueExpression{

	/**
	 *  First argument of the expression
	 */
	private final SymbolicExpression[] operands;
	
	/**
	 * Operator to apply
	 */
	private final NaryOperator operator;
	
	/**
	 * Builds the binary expression.
	 * 
	 * @param staticType the static type of this expression
	 * @param first      the first operand of this expression
	 * @param second     the second operand of this expression
	 * @param third      the third operand of this expression
	 * @param fourth	 the fourth operator of this expression
	 * @param operator   the operator to apply
	 * @param location   the code location of the statement that has generated
	 *                       this expression
	 */
	public NaryExpression(
			Type staticType,  
			SymbolicExpression[] operands,
			NaryOperator operator,
			CodeLocation location) {
		super(staticType, location);
		this.operands = operands;
		this.operator = operator;
		
	}
	
	/**
	 * Yields the i-th operand of this expression.
	 * 
	 * @return the i-th operand
	 */
	public SymbolicExpression getOperand(int i) {
		if( i > this.operands.length || i < 0)
			throw new IndexOutOfBoundsException();
		return this.operands[i];
	}
	
	/**
	 * Yields the operator that is applied to {@link #getFirst()},
	 * {@link #getSecond()}, {@link #getThird()} and {@link #getFourth()}.
	 * 
	 * @return the operator to apply
	 */
	public NaryOperator getOperator() {
		return operator;
	}
	
	@Override
	public SymbolicExpression pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression[] aux = this.operands;
		
		for (int i = 0; i < operands.length; ++i) {
			aux[i] = aux[i].pushScope(token, pp);
		}
		
		NaryExpression expr = new NaryExpression(
				getStaticType(),
				aux,
				operator,
				getCodeLocation());
		return expr;
	}

	@Override
	public SymbolicExpression popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		SymbolicExpression[] aux = this.operands;
		
		for (int i = 0; i < operands.length; ++i) {
			aux[i] = aux[i].popScope(token, pp);
		}
		
		NaryExpression expr = new NaryExpression(
				getStaticType(),
				aux,
				operator,
				getCodeLocation());
		return expr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((operator == null) ? 0 : operator.hashCode());
		
		for(SymbolicExpression expr : operands) {
			result = prime * result + ((expr == null) ? 0 : operator.hashCode());
		}
		
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
		NaryExpression other = (NaryExpression) obj;
		
		if (operands.length != other.operands.length) 
			return false;
		
		for(int i = 0; i < operands.length; ++i) {
			if(this.operands[i] == null) {
				if(other.operands[i] != null)
					return false;
			} else if (! this.operands[i].equals(other.operands[i]))
				return false;
		}
		if (operator != other.operator)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String res = operands[0] + " " + operator + "(";
		for( int i = 1; i < operands.length; ++i) {
			res = res + operands[i];
			if( i != operands.length - 1) {
				res = res + ", ";
			}
		}
		res = res + ")";
		return res;
	}
	
	@Override
	public <T> T accept(
			ExpressionVisitor<T> visitor,
			Object... params)
			throws SemanticException {
		T aux;
		aux = operands[0].accept(visitor, params);
		T[] op = (T[]) Array.newInstance(aux.getClass(), operands.length);
		op[0] = aux;
		for(int i = 1; i < operands.length; ++i) {
			aux = operands[i].accept(visitor, params);
			op[i] = aux;
		}
		return visitor.visit(this, op, params);
	}
	
	@Override
	public boolean mightNeedRewriting() {
		boolean res = false;
		
		for(SymbolicExpression expr : operands)
			res = res || expr.mightNeedRewriting();
		
		return res;
	}

	/**
	 * Yields a copy of this expression with the given operator.
	 * 
	 * @param operator the operator to apply to the left, middle, and right
	 *                     operands
	 * 
	 * @return the copy of this expression with the given operator
	 */
	public NaryExpression withOperator(
			NaryOperator operator) {
		return new NaryExpression(getStaticType(), operands, operator, getCodeLocation());
	}
	
	@Override
	public SymbolicExpression removeTypingExpressions() {
		SymbolicExpression[] aux = this.operands;
		
		for (int i = 0; i < operands.length; ++i) {
			aux[i] = aux[i].removeTypingExpressions();
		}
		
		boolean cond = true;
		
		for (int i = 0; i < operands.length; ++i) {
			cond = cond && (aux[i] == operands[i]);
		}
		
		if (cond)
			return this;
		return new NaryExpression(getStaticType(), aux, operator, getCodeLocation());
	}

	@Override
	public SymbolicExpression replace(
			SymbolicExpression source,
			SymbolicExpression target) {
		SymbolicExpression[] aux = this.operands;
		
		for (int i = 0; i < operands.length; ++i) {
			aux[i] = aux[i].replace(source,target);
		}
		
		boolean cond = true;
		
		for (int i = 0; i < operands.length; ++i) {
			cond = cond && (aux[i] == operands[i]);
		}
		
		if (cond)
			return this;
		return new NaryExpression(getStaticType(), aux, operator, getCodeLocation());
	}

}

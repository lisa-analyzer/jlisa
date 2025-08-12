package it.unive.jlisa.analysis;

import java.util.Set;

import it.unive.jlisa.lattices.ConstantValue;
import it.unive.jlisa.program.operator.JavaStringConcat;
import it.unive.jlisa.program.operator.JavaStringContains;
import it.unive.jlisa.program.operator.JavaStringLength;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;

public class ConstantPropagation implements BaseNonRelationalValueDomain<ConstantValue> {
		
	@Override
	public boolean canProcess(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression instanceof PushInv)
			// the type approximation of a pushinv is bottom, so the below check
			// will always fail regardless of the kind of value we are tracking
			return expression.getStaticType().isValueType();

		Set<Type> rts = null;
		try {
			rts = oracle.getRuntimeTypesOf(expression, pp);
		} catch (SemanticException e) {
			return false;
		}

		if (rts == null || rts.isEmpty())
			// if we have no runtime types, either the type domain has no type
			// information for the given expression (thus it can be anything,
			// also something that we can track) or the computation returned
			// bottom (and the whole state is likely going to go to bottom
			// anyway).
			return true;

		return rts.stream().anyMatch(Type::isValueType) || rts.stream().anyMatch( t -> t.toString().equals("String"));
	}

	@Override
	public ConstantValue evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle)
					throws SemanticException {
		// FIXME: remove constant
		return new ConstantValue(new Constant(NullType.INSTANCE, null, pp.getLocation()));
	}

	@Override
	public ConstantValue evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
					throws SemanticException {
		return new ConstantValue(constant.getValue());
	}


	@Override
	public ConstantValue evalUnaryExpression(UnaryExpression expression, ConstantValue arg, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		UnaryOperator operator = expression.getOperator();
		if (operator instanceof JavaStringLength && arg.getValue() instanceof String str)
			return new ConstantValue(str.length());
		return top();
	}

	@Override
	public ConstantValue evalBinaryExpression(
			BinaryExpression expression,
			ConstantValue left,
			ConstantValue right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();
			
			if (lVal instanceof String || rVal instanceof String) {
			} else if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() + ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() + ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() + ((Number) rVal).longValue());
			} else if (lVal instanceof Integer || rVal instanceof Integer) {
				return new ConstantValue(((Number) lVal).intValue() + ((Number) rVal).intValue());
			}
		}

		if (operator instanceof SubtractionOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() - ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() - ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() - ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() - ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof MultiplicationOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() * ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() * ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() * ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() * ((Number) rVal).intValue());
			}
		}
		
		if (operator instanceof DivisionOperator) {
			Object lVal = left.getValue();
			Object rVal = right.getValue();

			if (lVal instanceof Double || rVal instanceof Double) {
				return new ConstantValue(((Number) lVal).doubleValue() / ((Number) rVal).doubleValue());
			} else if (lVal instanceof Float || rVal instanceof Float) {
				return new ConstantValue(((Number) lVal).floatValue() / ((Number) rVal).floatValue());
			} else if (lVal instanceof Long || rVal instanceof Long) {
				return new ConstantValue(((Number) lVal).longValue() / ((Number) rVal).longValue());
			} else {
				return new ConstantValue(((Number) lVal).intValue() / ((Number) rVal).intValue());
			}
		}
				
		if (operator instanceof JavaStringConcat)
			return new ConstantValue(((String) left.getValue()) + ((String) right.getValue()));
		if (operator instanceof JavaStringContains) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return new ConstantValue(lv.contains(rv));			
		}
		
		return top();
	}

	@Override
	public Satisfiability satisfiesAbstractValue(ConstantValue value, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesAbstractValue(value, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesNullConstant(ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesNullConstant(pp, oracle);
	}

	@Override
	public Satisfiability satisfiesUnaryExpression(UnaryExpression expression, ConstantValue arg, ProgramPoint pp,
			SemanticOracle oracle) throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesUnaryExpression(expression, arg, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryExpression expression, ConstantValue left,
			ConstantValue right, ProgramPoint pp, SemanticOracle oracle) throws SemanticException {
		BinaryOperator operator = expression.getOperator();

		if (operator instanceof JavaStringContains) {
			String lv = ((String) left.getValue());
			String rv = ((String) right.getValue());
			return lv.contains(rv) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;			
		}
		
		return BaseNonRelationalValueDomain.super.satisfiesBinaryExpression(expression, left, right, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(TernaryExpression expression, ConstantValue left,
			ConstantValue middle, ConstantValue right, ProgramPoint pp, SemanticOracle oracle)
					throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesTernaryExpression(expression, left, middle, right, pp, oracle);
	}

	@Override
	public Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp, SemanticOracle oracle)
			throws SemanticException {
		// TODO Auto-generated method stub
		return BaseNonRelationalValueDomain.super.satisfiesNonNullConstant(constant, pp, oracle);
	}

	@Override
	public ConstantValue top() {
		return ConstantValue.TOP;
	}

	@Override
	public ConstantValue bottom() {
		return ConstantValue.BOTTOM;
	}
}

package it.unive.jlisa.program.operator;

import it.unive.lisa.symbolic.value.Operator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

public interface NaryOperator extends Operator {

	/**
	 * Computes the runtime types of this expression (i.e., of the result of
	 * this expression) assuming that the arguments of this expression have the
	 * given types.
	 * 
	 * @param types  the type system knowing about the types of the current
	 *                   program
	 * @param first  the set of types of the first argument of this expression
	 * @param second the set of types of the second argument of this expression
	 * @param third  the set of types of the third argument of this expression
	 * @param fourth the set of types of the fourth argument of this expression
	 * 
	 * @return the runtime types of this expression
	 */
	Set<Type> typeInference(
			TypeSystem types,
			Set<Type>[] operands);

}

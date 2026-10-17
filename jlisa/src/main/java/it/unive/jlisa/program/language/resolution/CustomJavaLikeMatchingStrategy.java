package it.unive.jlisa.program.language.resolution;

import it.unive.jlisa.program.cfg.JavaParameter;
import it.unive.jlisa.program.type.JavaArrayType;
import it.unive.jlisa.program.type.JavaClassType;
import it.unive.jlisa.program.type.JavaReferenceType;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

/**
 * A custom java strategy for handle unknown types in static type. If the static
 * type is untyped, we will fallback to the runtime type.
 */
public class CustomJavaLikeMatchingStrategy
	implements ParameterMatchingStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final CustomJavaLikeMatchingStrategy INSTANCE = new CustomJavaLikeMatchingStrategy();

	private CustomJavaLikeMatchingStrategy() {
	}

	@Override
	public final boolean matches(
			Call call,
			Parameter[] formals,
			Expression[] actuals,
			Set<Type>[] types) {

		if (formals.length == 0 && actuals.length > 0)
			return false;

		boolean varargs = false;
		if (formals.length > 0) {
			JavaParameter lastParameter = (JavaParameter) formals[formals.length - 1];
			varargs = lastParameter.isVarargs();
		}

		int fixed = varargs ? formals.length - 1 : formals.length;
		boolean nParamsMatch = varargs ? actuals.length >= fixed : actuals.length == formals.length;

		if (!nParamsMatch) {
			return false;
		}

		for (int i = 0; i < actuals.length; i++) {
			if (i < fixed) {
				if (!matches(call, i, formals[i], actuals[i], types[i]))
					return false;
			} else {
				if (!matchesVarargs(call, i, formals[formals.length - 1], actuals[i], types[i], i == fixed))
					return false;
			}
		}

		return true;

	}

	public boolean matches(
			Call call,
			int pos,
			Parameter formal,
			Expression actual,
			Set<Type> types) {
		if (call.getCallType() == CallType.INSTANCE && pos == 0)
			return matchReceiver(call, pos, formal, actual, types);
		return matchArgument(call, pos, formal, actual, types);
	}

	public boolean matchesVarargs(
			Call call,
			int pos,
			Parameter formal,
			Expression actual,
			Set<Type> types,
			boolean isLastArg) {
		return matchArgumentVarargs(call, pos, formal, actual, types, isLastArg);
	}

	private boolean matchReceiver(
			Call call,
			int pos,
			Parameter formal,
			Expression actual,
			Set<Type> types) {
		return types.stream().anyMatch(rt -> rt.canBeAssignedTo(formal.getStaticType()));
	}

	private boolean matchArgument(
			Call call,
			int pos,
			Parameter formal,
			Expression actual,
			Set<Type> types) {

		if (!actual.getStaticType().equals(Untyped.INSTANCE)
				&& actual.getStaticType().canBeAssignedTo(formal.getStaticType()))
			return true;

		// assert (formal instanceof JavaParameter);
		// JavaParameter parameter = (JavaParameter) formal;
		//
		// if (parameter.isVarargs()) {
		// 	// TODO: important, convert formal to base only if the actual is not directly
		// 	// an array. Also in that case, type conversions are not applicable
		// 	assert (parameter.getStaticType() instanceof JavaReferenceType);
		// 	JavaArrayType arrType = (JavaArrayType) ((JavaReferenceType) parameter.getStaticType()).getInnerType();
		// 	Type base = arrType.getBaseType();
		// 	return base.equals(actual.getStaticType());
		// }

		for (Type rType : types)
			if (rType.canBeAssignedTo(formal.getStaticType()))
				// equal or widening
				return true;
			else if (JavaClassType.isWrapperOf(formal.getStaticType(), rType))
				// boxing
				return true;
			else if (JavaClassType.isWrapperOf(rType, formal.getStaticType()))
				// unboxing
				return true;
		// TODO the next case should be allowed only when we handle it in the
		// assigning strategy and in the call graph's parameter distance
		// Type unwrapped;
		// else if ((unwrapped = JavaClassType.getUnwrappedType(rType)) != null)
		// unboxing + widening
		// if (unwrapped.canBeAssignedTo(formal.getStaticType()))
		// return true;

		return false;
	}

	private boolean matchArgumentVarargs(
			Call call,
			int pos,
			Parameter formal,
			Expression actual,
			Set<Type> types,
			boolean isLastArg) {

		// array as last argument is okay in the case of varargs.
		// but need to check whether it's last or not to prevent cases like the following:
		// f(String... s) matched with f(new String[], new String[])
		if (!actual.getStaticType().equals(Untyped.INSTANCE)
				&& isLastArg && actual.getStaticType().canBeAssignedTo(formal.getStaticType()))
			return true;

		assert (formal instanceof JavaParameter);
		JavaParameter parameter = (JavaParameter) formal;
		assert (parameter.isVarargs());

		assert (parameter.getStaticType() instanceof JavaReferenceType);
		JavaArrayType arrType = (JavaArrayType) ((JavaReferenceType) parameter.getStaticType()).getInnerType();
		Type base = arrType.getBaseType();

		for (Type rType : types)
			if (rType.canBeAssignedTo(base))
				// equal or widening
				return true;
			else if (JavaClassType.isWrapperOf(base, rType))
				// boxing
				return true;
			else if (JavaClassType.isWrapperOf(rType, base))
				// unboxing
				return true;

		// TODO the next case should be allowed only when we handle it in the
		// assigning strategy and in the call graph's parameter distance
		// Type unwrapped;
		// else if ((unwrapped = JavaClassType.getUnwrappedType(rType)) != null)
		// unboxing + widening
		// if (unwrapped.canBeAssignedTo(formal.getStaticType()))
		// return true;

		return false;
	}

}

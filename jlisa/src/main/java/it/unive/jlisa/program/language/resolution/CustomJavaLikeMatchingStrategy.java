package it.unive.jlisa.program.language.resolution;

import it.unive.jlisa.program.type.JavaClassType;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.language.resolution.FixedOrderMatchingStrategy;
import it.unive.lisa.program.language.resolution.RuntimeTypesMatchingStrategy;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

/**
 * A custom java strategy for handle unknown types in static type. If the static
 * type is untyped, we will fallback to the runtime type.
 */
public class CustomJavaLikeMatchingStrategy
		extends
		FixedOrderMatchingStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final CustomJavaLikeMatchingStrategy INSTANCE = new CustomJavaLikeMatchingStrategy();

	private CustomJavaLikeMatchingStrategy() {
	}

	@Override
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
		if (actual.getStaticType().equals(Untyped.INSTANCE))
			return RuntimeTypesMatchingStrategy.INSTANCE.matches(call, pos, formal, actual, types);

		if (actual.getStaticType().canBeAssignedTo(formal.getStaticType()))
			return true;

		for (Type rType : types)
			if (JavaClassType.isWrapperOf(formal.getStaticType(), rType)) {
				// boxing
				return true;
			} else if (JavaClassType.isWrapperOf(rType, formal.getStaticType())) {
				// unboxing
				return true;
			}

		return false;
	}

}

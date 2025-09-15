package it.unive.jlisa.program.language.resolution;

import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.language.resolution.FixedOrderMatchingStrategy;
import it.unive.lisa.program.language.resolution.RuntimeTypesMatchingStrategy;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

public class JavaStaticTypeMatchingStrategy
		extends
		FixedOrderMatchingStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final JavaStaticTypeMatchingStrategy INSTANCE = new JavaStaticTypeMatchingStrategy();

	private JavaStaticTypeMatchingStrategy() {
	}

	@Override
	public boolean matches(
			Call call,
			int pos,
			Parameter formal,
			Expression actual,
			Set<Type> types) {
		if (actual.getStaticType().equals(Untyped.INSTANCE)) {
			return RuntimeTypesMatchingStrategy.INSTANCE.matches(call, pos, formal, actual, types);
		}
		return actual.getStaticType().canBeAssignedTo(formal.getStaticType());
	}
}
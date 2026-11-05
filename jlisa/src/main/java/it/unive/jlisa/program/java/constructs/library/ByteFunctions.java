package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of {@code java.lang.Byte} computed by
 * {@link JavaLibraryFunction}s.
 */
public final class ByteFunctions {

	private ByteFunctions() {
	}

	public static class CompareTo extends BoxedComparison {

		public CompareTo(
				CFG cfg,
				CodeLocation location,
				Expression receiver,
				Expression other) {
			super(cfg, location, "java.lang.Byte", JavaByteType.INSTANCE, JavaLibraryFunction.BYTE_COMPARE, receiver, other);
		}

		public static CompareTo build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CompareTo(cfg, location, params[0], params[1]);
		}
	}
}

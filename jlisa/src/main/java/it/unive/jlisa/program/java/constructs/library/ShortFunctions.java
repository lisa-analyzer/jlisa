package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of {@code java.lang.Short} computed by
 * {@link JavaLibraryFunction}s.
 */
public final class ShortFunctions {

	private ShortFunctions() {
	}

	public static class ReverseBytes extends StaticUnaryFunction {

		public ReverseBytes(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.SHORT_REVERSE_BYTES, arg);
		}

		public static ReverseBytes build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ReverseBytes(cfg, location, params[0]);
		}
	}

	public static class ToUnsignedInt extends StaticUnaryFunction {

		public ToUnsignedInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.SHORT_TO_UNSIGNED_INT, arg);
		}

		public static ToUnsignedInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToUnsignedInt(cfg, location, params[0]);
		}
	}

	public static class ToUnsignedLong extends StaticUnaryFunction {

		public ToUnsignedLong(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.SHORT_TO_UNSIGNED_LONG, arg);
		}

		public static ToUnsignedLong build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToUnsignedLong(cfg, location, params[0]);
		}
	}

	public static class CompareTo extends BoxedComparison {

		public CompareTo(
				CFG cfg,
				CodeLocation location,
				Expression receiver,
				Expression other) {
			super(cfg, location, "java.lang.Short", JavaShortType.INSTANCE, JavaLibraryFunction.SHORT_COMPARE, receiver, other);
		}

		public static CompareTo build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CompareTo(cfg, location, params[0], params[1]);
		}
	}
}

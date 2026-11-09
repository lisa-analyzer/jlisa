package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of {@code java.lang.Boolean} computed by
 * {@link JavaLibraryFunction}s.
 */
public final class BooleanFunctions {

	private BooleanFunctions() {
	}

	public static class Compare extends StaticBinaryFunction {

		public Compare(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.BOOLEAN_COMPARE, left, right);
		}

		public static Compare build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Compare(cfg, location, params[0], params[1]);
		}
	}

	public static class LogicalAnd extends StaticBinaryFunction {

		public LogicalAnd(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.BOOLEAN_LOGICAL_AND, left, right);
		}

		public static LogicalAnd build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LogicalAnd(cfg, location, params[0], params[1]);
		}
	}

	public static class LogicalOr extends StaticBinaryFunction {

		public LogicalOr(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.BOOLEAN_LOGICAL_OR, left, right);
		}

		public static LogicalOr build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LogicalOr(cfg, location, params[0], params[1]);
		}
	}

	public static class LogicalXor extends StaticBinaryFunction {

		public LogicalXor(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.BOOLEAN_LOGICAL_XOR, left, right);
		}

		public static LogicalXor build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LogicalXor(cfg, location, params[0], params[1]);
		}
	}

	public static class CompareTo extends BoxedComparison {

		public CompareTo(
				CFG cfg,
				CodeLocation location,
				Expression receiver,
				Expression other) {
			super(cfg, location, "java.lang.Boolean", JavaBooleanType.INSTANCE, JavaLibraryFunction.BOOLEAN_COMPARE,
					receiver, other);
		}

		public static CompareTo build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CompareTo(cfg, location, params[0], params[1]);
		}
	}
}

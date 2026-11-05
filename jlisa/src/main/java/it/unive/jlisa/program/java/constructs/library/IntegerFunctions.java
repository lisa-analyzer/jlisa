package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of {@code java.lang.Integer} computed by
 * {@link JavaLibraryFunction}s.
 */
public final class IntegerFunctions {

	private IntegerFunctions() {
	}

	public static class BitCount extends StaticUnaryFunction {

		public BitCount(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_BIT_COUNT, arg);
		}

		public static BitCount build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new BitCount(cfg, location, params[0]);
		}
	}

	public static class CompareUnsigned extends StaticBinaryFunction {

		public CompareUnsigned(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.INTEGER_COMPARE_UNSIGNED, left, right);
		}

		public static CompareUnsigned build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CompareUnsigned(cfg, location, params[0], params[1]);
		}
	}

	public static class HighestOneBit extends StaticUnaryFunction {

		public HighestOneBit(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_HIGHEST_ONE_BIT, arg);
		}

		public static HighestOneBit build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new HighestOneBit(cfg, location, params[0]);
		}
	}

	public static class LowestOneBit extends StaticUnaryFunction {

		public LowestOneBit(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_LOWEST_ONE_BIT, arg);
		}

		public static LowestOneBit build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LowestOneBit(cfg, location, params[0]);
		}
	}

	public static class Max extends StaticBinaryFunction {

		public Max(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.INTEGER_MAX, left, right);
		}

		public static Max build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Max(cfg, location, params[0], params[1]);
		}
	}

	public static class Min extends StaticBinaryFunction {

		public Min(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.INTEGER_MIN, left, right);
		}

		public static Min build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Min(cfg, location, params[0], params[1]);
		}
	}

	public static class NumberOfLeadingZeros extends StaticUnaryFunction {

		public NumberOfLeadingZeros(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_NUMBER_OF_LEADING_ZEROS, arg);
		}

		public static NumberOfLeadingZeros build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new NumberOfLeadingZeros(cfg, location, params[0]);
		}
	}

	public static class NumberOfTrailingZeros extends StaticUnaryFunction {

		public NumberOfTrailingZeros(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_NUMBER_OF_TRAILING_ZEROS, arg);
		}

		public static NumberOfTrailingZeros build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new NumberOfTrailingZeros(cfg, location, params[0]);
		}
	}

	public static class Reverse extends StaticUnaryFunction {

		public Reverse(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_REVERSE, arg);
		}

		public static Reverse build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Reverse(cfg, location, params[0]);
		}
	}

	public static class ReverseBytes extends StaticUnaryFunction {

		public ReverseBytes(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_REVERSE_BYTES, arg);
		}

		public static ReverseBytes build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ReverseBytes(cfg, location, params[0]);
		}
	}

	public static class RotateLeft extends StaticBinaryFunction {

		public RotateLeft(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.INTEGER_ROTATE_LEFT, left, right);
		}

		public static RotateLeft build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new RotateLeft(cfg, location, params[0], params[1]);
		}
	}

	public static class RotateRight extends StaticBinaryFunction {

		public RotateRight(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.INTEGER_ROTATE_RIGHT, left, right);
		}

		public static RotateRight build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new RotateRight(cfg, location, params[0], params[1]);
		}
	}

	public static class Signum extends StaticUnaryFunction {

		public Signum(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_SIGNUM, arg);
		}

		public static Signum build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Signum(cfg, location, params[0]);
		}
	}

	public static class Sum extends StaticBinaryFunction {

		public Sum(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.INTEGER_SUM, left, right);
		}

		public static Sum build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Sum(cfg, location, params[0], params[1]);
		}
	}

	public static class ToUnsignedLong extends StaticUnaryFunction {

		public ToUnsignedLong(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_UNSIGNED_LONG, arg);
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
			super(cfg, location, "java.lang.Integer", JavaIntType.INSTANCE, JavaLibraryFunction.INTEGER_COMPARE, receiver, other);
		}

		public static CompareTo build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CompareTo(cfg, location, params[0], params[1]);
		}
	}
}

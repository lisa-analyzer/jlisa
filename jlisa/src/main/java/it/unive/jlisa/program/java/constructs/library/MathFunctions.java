package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of {@code java.lang.Math} computed by
 * {@link JavaLibraryFunction}s.
 */
public final class MathFunctions {

	private MathFunctions() {
	}

	public static class GetExponentDouble extends StaticUnaryFunction {

		public GetExponentDouble(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.MATH_GET_EXPONENT_DOUBLE, arg);
		}

		public static GetExponentDouble build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetExponentDouble(cfg, location, params[0]);
		}
	}

	public static class GetExponentFloat extends StaticUnaryFunction {

		public GetExponentFloat(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.MATH_GET_EXPONENT_FLOAT, arg);
		}

		public static GetExponentFloat build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetExponentFloat(cfg, location, params[0]);
		}
	}
}

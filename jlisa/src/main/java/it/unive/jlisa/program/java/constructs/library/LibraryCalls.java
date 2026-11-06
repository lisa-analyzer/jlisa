package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaBooleanType;
import it.unive.jlisa.program.type.JavaByteType;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.jlisa.program.type.JavaDoubleType;
import it.unive.jlisa.program.type.JavaFloatType;
import it.unive.jlisa.program.type.JavaIntType;
import it.unive.jlisa.program.type.JavaLongType;
import it.unive.jlisa.program.type.JavaShortType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of the Java library computed by
 * {@link JavaLibraryFunction}s over primitive values, strings and instances of
 * wrapper classes (see {@link LibraryCall}).
 */
public final class LibraryCalls {

	private LibraryCalls() {
	}

	public static class IntegerToString extends LibraryCall {

		public IntegerToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static IntegerToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToString(cfg, location, params[0]);
		}
	}

	public static class IntegerToStringRadix extends LibraryCall {

		public IntegerToStringRadix(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_STRING_RADIX, new Arg[] { Arg.VALUE, Arg.VALUE },
					p0, p1);
		}

		public static IntegerToStringRadix build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToStringRadix(cfg, location, params[0], params[1]);
		}
	}

	public static class IntegerToUnsignedString extends LibraryCall {

		public IntegerToUnsignedString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_UNSIGNED_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static IntegerToUnsignedString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToUnsignedString(cfg, location, params[0]);
		}
	}

	public static class IntegerToUnsignedStringRadix extends LibraryCall {

		public IntegerToUnsignedStringRadix(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_UNSIGNED_STRING_RADIX,
					new Arg[] { Arg.VALUE, Arg.VALUE },
					p0, p1);
		}

		public static IntegerToUnsignedStringRadix build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToUnsignedStringRadix(cfg, location, params[0], params[1]);
		}
	}

	public static class IntegerToHexString extends LibraryCall {

		public IntegerToHexString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_HEX_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static IntegerToHexString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToHexString(cfg, location, params[0]);
		}
	}

	public static class IntegerToBinaryString extends LibraryCall {

		public IntegerToBinaryString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_BINARY_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static IntegerToBinaryString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToBinaryString(cfg, location, params[0]);
		}
	}

	public static class IntegerToOctalString extends LibraryCall {

		public IntegerToOctalString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_OCTAL_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static IntegerToOctalString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToOctalString(cfg, location, params[0]);
		}
	}

	public static class LongToString extends LibraryCall {

		public LongToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static LongToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToString(cfg, location, params[0]);
		}
	}

	public static class LongToStringRadix extends LibraryCall {

		public LongToStringRadix(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_STRING_RADIX, new Arg[] { Arg.VALUE, Arg.VALUE },
					p0, p1);
		}

		public static LongToStringRadix build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToStringRadix(cfg, location, params[0], params[1]);
		}
	}

	public static class LongToUnsignedString extends LibraryCall {

		public LongToUnsignedString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_UNSIGNED_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static LongToUnsignedString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToUnsignedString(cfg, location, params[0]);
		}
	}

	public static class LongToUnsignedStringRadix extends LibraryCall {

		public LongToUnsignedStringRadix(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_UNSIGNED_STRING_RADIX, new Arg[] { Arg.VALUE, Arg.VALUE },
					p0, p1);
		}

		public static LongToUnsignedStringRadix build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToUnsignedStringRadix(cfg, location, params[0], params[1]);
		}
	}

	public static class LongToHexString extends LibraryCall {

		public LongToHexString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_HEX_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static LongToHexString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToHexString(cfg, location, params[0]);
		}
	}

	public static class LongToBinaryString extends LibraryCall {

		public LongToBinaryString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_BINARY_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static LongToBinaryString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToBinaryString(cfg, location, params[0]);
		}
	}

	public static class LongToOctalString extends LibraryCall {

		public LongToOctalString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_OCTAL_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static LongToOctalString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToOctalString(cfg, location, params[0]);
		}
	}

	public static class ShortToString extends LibraryCall {

		public ShortToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.SHORT_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static ShortToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ShortToString(cfg, location, params[0]);
		}
	}

	public static class ByteToString extends LibraryCall {

		public ByteToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.BYTE_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static ByteToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ByteToString(cfg, location, params[0]);
		}
	}

	public static class BooleanToString extends LibraryCall {

		public BooleanToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.BOOLEAN_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static BooleanToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new BooleanToString(cfg, location, params[0]);
		}
	}

	public static class CharacterToString extends LibraryCall {

		public CharacterToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static CharacterToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CharacterToString(cfg, location, params[0]);
		}
	}

	public static class DoubleToString extends LibraryCall {

		public DoubleToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.DOUBLE_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static DoubleToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new DoubleToString(cfg, location, params[0]);
		}
	}

	public static class FloatToString extends LibraryCall {

		public FloatToString(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.FLOAT_TO_STRING, new Arg[] { Arg.VALUE },
					p0);
		}

		public static FloatToString build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new FloatToString(cfg, location, params[0]);
		}
	}

	public static class IntegerToStringInstance extends LibraryCall {

		public IntegerToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.INTEGER_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Integer", JavaIntType.INSTANCE) },
					p0);
		}

		public static IntegerToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IntegerToStringInstance(cfg, location, params[0]);
		}
	}

	public static class LongToStringInstance extends LibraryCall {

		public LongToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.LONG_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Long", JavaLongType.INSTANCE) },
					p0);
		}

		public static LongToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new LongToStringInstance(cfg, location, params[0]);
		}
	}

	public static class ShortToStringInstance extends LibraryCall {

		public ShortToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.SHORT_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Short", JavaShortType.INSTANCE) },
					p0);
		}

		public static ShortToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ShortToStringInstance(cfg, location, params[0]);
		}
	}

	public static class ByteToStringInstance extends LibraryCall {

		public ByteToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.BYTE_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Byte", JavaByteType.INSTANCE) },
					p0);
		}

		public static ByteToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ByteToStringInstance(cfg, location, params[0]);
		}
	}

	public static class BooleanToStringInstance extends LibraryCall {

		public BooleanToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.BOOLEAN_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Boolean", JavaBooleanType.INSTANCE) },
					p0);
		}

		public static BooleanToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new BooleanToStringInstance(cfg, location, params[0]);
		}
	}

	public static class CharacterToStringInstance extends LibraryCall {

		public CharacterToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Character", JavaCharType.INSTANCE) },
					p0);
		}

		public static CharacterToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CharacterToStringInstance(cfg, location, params[0]);
		}
	}

	public static class DoubleToStringInstance extends LibraryCall {

		public DoubleToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.DOUBLE_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Double", JavaDoubleType.INSTANCE) },
					p0);
		}

		public static DoubleToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new DoubleToStringInstance(cfg, location, params[0]);
		}
	}

	public static class FloatToStringInstance extends LibraryCall {

		public FloatToStringInstance(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.FLOAT_TO_STRING,
					new Arg[] { Arg.boxed("java.lang.Float", JavaFloatType.INSTANCE) },
					p0);
		}

		public static FloatToStringInstance build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new FloatToStringInstance(cfg, location, params[0]);
		}
	}

	public static class DoubleCompareTo extends LibraryCall {

		public DoubleCompareTo(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.DOUBLE_COMPARE,
					new Arg[] { Arg.boxed("java.lang.Double", JavaDoubleType.INSTANCE),
							Arg.boxed("java.lang.Double", JavaDoubleType.INSTANCE) },
					p0, p1);
		}

		public static DoubleCompareTo build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new DoubleCompareTo(cfg, location, params[0], params[1]);
		}
	}

	public static class FloatCompareTo extends LibraryCall {

		public FloatCompareTo(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.FLOAT_COMPARE,
					new Arg[] { Arg.boxed("java.lang.Float", JavaFloatType.INSTANCE),
							Arg.boxed("java.lang.Float", JavaFloatType.INSTANCE) },
					p0, p1);
		}

		public static FloatCompareTo build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new FloatCompareTo(cfg, location, params[0], params[1]);
		}
	}

	public static class StringIsEmpty extends LibraryCall {

		public StringIsEmpty(
				CFG cfg,
				CodeLocation location,
				Expression p0) {
			super(cfg, location, JavaLibraryFunction.STRING_IS_EMPTY, new Arg[] { Arg.STRING },
					p0);
		}

		public static StringIsEmpty build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringIsEmpty(cfg, location, params[0]);
		}
	}

	public static class StringIndexOfInt extends LibraryCall {

		public StringIndexOfInt(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.STRING_INDEX_OF_CHAR, new Arg[] { Arg.STRING, Arg.VALUE },
					p0, p1);
		}

		public static StringIndexOfInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringIndexOfInt(cfg, location, params[0], params[1]);
		}
	}

	public static class StringIndexOfIntFrom extends LibraryCall {

		public StringIndexOfIntFrom(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1,
				Expression p2) {
			super(cfg, location, JavaLibraryFunction.STRING_INDEX_OF_CHAR_FROM,
					new Arg[] { Arg.STRING, Arg.VALUE, Arg.VALUE },
					p0, p1, p2);
		}

		public static StringIndexOfIntFrom build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringIndexOfIntFrom(cfg, location, params[0], params[1], params[2]);
		}
	}

	public static class StringLastIndexOfInt extends LibraryCall {

		public StringLastIndexOfInt(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.STRING_LAST_INDEX_OF_CHAR, new Arg[] { Arg.STRING, Arg.VALUE },
					p0, p1);
		}

		public static StringLastIndexOfInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringLastIndexOfInt(cfg, location, params[0], params[1]);
		}
	}

	public static class StringLastIndexOfIntFrom extends LibraryCall {

		public StringLastIndexOfIntFrom(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1,
				Expression p2) {
			super(cfg, location, JavaLibraryFunction.STRING_LAST_INDEX_OF_CHAR_FROM,
					new Arg[] { Arg.STRING, Arg.VALUE, Arg.VALUE },
					p0, p1, p2);
		}

		public static StringLastIndexOfIntFrom build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringLastIndexOfIntFrom(cfg, location, params[0], params[1], params[2]);
		}
	}

	public static class StringCompareToIgnoreCase extends LibraryCall {

		public StringCompareToIgnoreCase(
				CFG cfg,
				CodeLocation location,
				Expression p0,
				Expression p1) {
			super(cfg, location, JavaLibraryFunction.STRING_COMPARE_TO_IGNORE_CASE,
					new Arg[] { Arg.STRING, Arg.STRING },
					p0, p1);
		}

		public static StringCompareToIgnoreCase build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new StringCompareToIgnoreCase(cfg, location, params[0], params[1]);
		}
	}
}

package it.unive.jlisa.program.java.constructs.library;

import it.unive.jlisa.program.operator.JavaLibraryFunction;
import it.unive.jlisa.program.type.JavaCharType;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Expression;

/**
 * Models of methods of {@code java.lang.Character} computed by
 * {@link JavaLibraryFunction}s.
 */
public final class CharacterFunctions {

	private CharacterFunctions() {
	}

	public static class Compare extends StaticBinaryFunction {

		public Compare(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_COMPARE, left, right);
		}

		public static Compare build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new Compare(cfg, location, params[0], params[1]);
		}
	}

	public static class CharCount extends StaticUnaryFunction {

		public CharCount(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_CHAR_COUNT, arg);
		}

		public static CharCount build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new CharCount(cfg, location, params[0]);
		}
	}

	public static class ToCodePoint extends StaticBinaryFunction {

		public ToCodePoint(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_CODE_POINT, left, right);
		}

		public static ToCodePoint build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToCodePoint(cfg, location, params[0], params[1]);
		}
	}

	public static class DigitInt extends StaticBinaryFunction {

		public DigitInt(
				CFG cfg,
				CodeLocation location,
				Expression left,
				Expression right) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_DIGIT, left, right);
		}

		public static DigitInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new DigitInt(cfg, location, params[0], params[1]);
		}
	}

	public static class GetDirectionalityInt extends StaticUnaryFunction {

		public GetDirectionalityInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_GET_DIRECTIONALITY, arg);
		}

		public static GetDirectionalityInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetDirectionalityInt(cfg, location, params[0]);
		}
	}

	public static class GetDirectionalityChar extends StaticUnaryFunction {

		public GetDirectionalityChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_GET_DIRECTIONALITY, arg);
		}

		public static GetDirectionalityChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetDirectionalityChar(cfg, location, params[0]);
		}
	}

	public static class GetNumericValueInt extends StaticUnaryFunction {

		public GetNumericValueInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_GET_NUMERIC_VALUE, arg);
		}

		public static GetNumericValueInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetNumericValueInt(cfg, location, params[0]);
		}
	}

	public static class GetNumericValueChar extends StaticUnaryFunction {

		public GetNumericValueChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_GET_NUMERIC_VALUE, arg);
		}

		public static GetNumericValueChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetNumericValueChar(cfg, location, params[0]);
		}
	}

	public static class GetTypeInt extends StaticUnaryFunction {

		public GetTypeInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_GET_TYPE, arg);
		}

		public static GetTypeInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetTypeInt(cfg, location, params[0]);
		}
	}

	public static class GetTypeChar extends StaticUnaryFunction {

		public GetTypeChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_GET_TYPE, arg);
		}

		public static GetTypeChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new GetTypeChar(cfg, location, params[0]);
		}
	}

	public static class ToLowerCaseInt extends StaticUnaryFunction {

		public ToLowerCaseInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_LOWER_CASE, arg);
		}

		public static ToLowerCaseInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToLowerCaseInt(cfg, location, params[0]);
		}
	}

	public static class ToUpperCaseInt extends StaticUnaryFunction {

		public ToUpperCaseInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_UPPER_CASE, arg);
		}

		public static ToUpperCaseInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToUpperCaseInt(cfg, location, params[0]);
		}
	}

	public static class ToTitleCaseInt extends StaticUnaryFunction {

		public ToTitleCaseInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_TITLE_CASE, arg);
		}

		public static ToTitleCaseInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToTitleCaseInt(cfg, location, params[0]);
		}
	}

	public static class ToTitleCaseChar extends StaticUnaryFunction {

		public ToTitleCaseChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_TO_TITLE_CASE_CHAR, arg);
		}

		public static ToTitleCaseChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new ToTitleCaseChar(cfg, location, params[0]);
		}
	}

	public static class IsJavaLetter extends StaticUnaryFunction {

		public IsJavaLetter(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_JAVA_LETTER, arg);
		}

		public static IsJavaLetter build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsJavaLetter(cfg, location, params[0]);
		}
	}

	public static class IsJavaLetterOrDigit extends StaticUnaryFunction {

		public IsJavaLetterOrDigit(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_JAVA_LETTER_OR_DIGIT, arg);
		}

		public static IsJavaLetterOrDigit build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsJavaLetterOrDigit(cfg, location, params[0]);
		}
	}

	public static class IsSpace extends StaticUnaryFunction {

		public IsSpace(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_SPACE, arg);
		}

		public static IsSpace build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsSpace(cfg, location, params[0]);
		}
	}

	public static class IsAlphabeticInt extends StaticUnaryFunction {

		public IsAlphabeticInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_ALPHABETIC, arg);
		}

		public static IsAlphabeticInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsAlphabeticInt(cfg, location, params[0]);
		}
	}

	public static class IsBmpCodePointInt extends StaticUnaryFunction {

		public IsBmpCodePointInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_BMP_CODE_POINT, arg);
		}

		public static IsBmpCodePointInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsBmpCodePointInt(cfg, location, params[0]);
		}
	}

	public static class IsDefinedInt extends StaticUnaryFunction {

		public IsDefinedInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_DEFINED, arg);
		}

		public static IsDefinedInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsDefinedInt(cfg, location, params[0]);
		}
	}

	public static class IsDigitInt extends StaticUnaryFunction {

		public IsDigitInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_DIGIT, arg);
		}

		public static IsDigitInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsDigitInt(cfg, location, params[0]);
		}
	}

	public static class IsIdentifierIgnorableInt extends StaticUnaryFunction {

		public IsIdentifierIgnorableInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_IDENTIFIER_IGNORABLE, arg);
		}

		public static IsIdentifierIgnorableInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsIdentifierIgnorableInt(cfg, location, params[0]);
		}
	}

	public static class IsIdentifierIgnorableChar extends StaticUnaryFunction {

		public IsIdentifierIgnorableChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_IDENTIFIER_IGNORABLE, arg);
		}

		public static IsIdentifierIgnorableChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsIdentifierIgnorableChar(cfg, location, params[0]);
		}
	}

	public static class IsIdeographicInt extends StaticUnaryFunction {

		public IsIdeographicInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_IDEOGRAPHIC, arg);
		}

		public static IsIdeographicInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsIdeographicInt(cfg, location, params[0]);
		}
	}

	public static class IsISOControlInt extends StaticUnaryFunction {

		public IsISOControlInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_ISO_CONTROL, arg);
		}

		public static IsISOControlInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsISOControlInt(cfg, location, params[0]);
		}
	}

	public static class IsISOControlChar extends StaticUnaryFunction {

		public IsISOControlChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_ISO_CONTROL, arg);
		}

		public static IsISOControlChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsISOControlChar(cfg, location, params[0]);
		}
	}

	public static class IsJavaIdentifierPartInt extends StaticUnaryFunction {

		public IsJavaIdentifierPartInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_JAVA_IDENTIFIER_PART, arg);
		}

		public static IsJavaIdentifierPartInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsJavaIdentifierPartInt(cfg, location, params[0]);
		}
	}

	public static class IsJavaIdentifierStartInt extends StaticUnaryFunction {

		public IsJavaIdentifierStartInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_JAVA_IDENTIFIER_START, arg);
		}

		public static IsJavaIdentifierStartInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsJavaIdentifierStartInt(cfg, location, params[0]);
		}
	}

	public static class IsLetterInt extends StaticUnaryFunction {

		public IsLetterInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_LETTER, arg);
		}

		public static IsLetterInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsLetterInt(cfg, location, params[0]);
		}
	}

	public static class IsLetterOrDigitInt extends StaticUnaryFunction {

		public IsLetterOrDigitInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_LETTER_OR_DIGIT, arg);
		}

		public static IsLetterOrDigitInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsLetterOrDigitInt(cfg, location, params[0]);
		}
	}

	public static class IsLowerCaseInt extends StaticUnaryFunction {

		public IsLowerCaseInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_LOWER_CASE, arg);
		}

		public static IsLowerCaseInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsLowerCaseInt(cfg, location, params[0]);
		}
	}

	public static class IsMirroredInt extends StaticUnaryFunction {

		public IsMirroredInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_MIRRORED, arg);
		}

		public static IsMirroredInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsMirroredInt(cfg, location, params[0]);
		}
	}

	public static class IsMirroredChar extends StaticUnaryFunction {

		public IsMirroredChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_MIRRORED, arg);
		}

		public static IsMirroredChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsMirroredChar(cfg, location, params[0]);
		}
	}

	public static class IsSpaceCharInt extends StaticUnaryFunction {

		public IsSpaceCharInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_SPACE_CHAR, arg);
		}

		public static IsSpaceCharInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsSpaceCharInt(cfg, location, params[0]);
		}
	}

	public static class IsSpaceCharChar extends StaticUnaryFunction {

		public IsSpaceCharChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_SPACE_CHAR, arg);
		}

		public static IsSpaceCharChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsSpaceCharChar(cfg, location, params[0]);
		}
	}

	public static class IsSupplementaryCodePointInt extends StaticUnaryFunction {

		public IsSupplementaryCodePointInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_SUPPLEMENTARY_CODE_POINT, arg);
		}

		public static IsSupplementaryCodePointInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsSupplementaryCodePointInt(cfg, location, params[0]);
		}
	}

	public static class IsTitleCaseInt extends StaticUnaryFunction {

		public IsTitleCaseInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_TITLE_CASE, arg);
		}

		public static IsTitleCaseInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsTitleCaseInt(cfg, location, params[0]);
		}
	}

	public static class IsTitleCaseChar extends StaticUnaryFunction {

		public IsTitleCaseChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_TITLE_CASE, arg);
		}

		public static IsTitleCaseChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsTitleCaseChar(cfg, location, params[0]);
		}
	}

	public static class IsUnicodeIdentifierPartInt extends StaticUnaryFunction {

		public IsUnicodeIdentifierPartInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_UNICODE_IDENTIFIER_PART, arg);
		}

		public static IsUnicodeIdentifierPartInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsUnicodeIdentifierPartInt(cfg, location, params[0]);
		}
	}

	public static class IsUnicodeIdentifierPartChar extends StaticUnaryFunction {

		public IsUnicodeIdentifierPartChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_UNICODE_IDENTIFIER_PART, arg);
		}

		public static IsUnicodeIdentifierPartChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsUnicodeIdentifierPartChar(cfg, location, params[0]);
		}
	}

	public static class IsUnicodeIdentifierStartInt extends StaticUnaryFunction {

		public IsUnicodeIdentifierStartInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_UNICODE_IDENTIFIER_START, arg);
		}

		public static IsUnicodeIdentifierStartInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsUnicodeIdentifierStartInt(cfg, location, params[0]);
		}
	}

	public static class IsUnicodeIdentifierStartChar extends StaticUnaryFunction {

		public IsUnicodeIdentifierStartChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_UNICODE_IDENTIFIER_START, arg);
		}

		public static IsUnicodeIdentifierStartChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsUnicodeIdentifierStartChar(cfg, location, params[0]);
		}
	}

	public static class IsUpperCaseInt extends StaticUnaryFunction {

		public IsUpperCaseInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_UPPER_CASE, arg);
		}

		public static IsUpperCaseInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsUpperCaseInt(cfg, location, params[0]);
		}
	}

	public static class IsValidCodePointInt extends StaticUnaryFunction {

		public IsValidCodePointInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_VALID_CODE_POINT, arg);
		}

		public static IsValidCodePointInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsValidCodePointInt(cfg, location, params[0]);
		}
	}

	public static class IsWhitespaceInt extends StaticUnaryFunction {

		public IsWhitespaceInt(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_WHITESPACE, arg);
		}

		public static IsWhitespaceInt build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsWhitespaceInt(cfg, location, params[0]);
		}
	}

	public static class IsWhitespaceChar extends StaticUnaryFunction {

		public IsWhitespaceChar(
				CFG cfg,
				CodeLocation location,
				Expression arg) {
			super(cfg, location, JavaLibraryFunction.CHARACTER_IS_WHITESPACE, arg);
		}

		public static IsWhitespaceChar build(
				CFG cfg,
				CodeLocation location,
				Expression... params) {
			return new IsWhitespaceChar(cfg, location, params[0]);
		}
	}

	public static class CompareTo extends BoxedComparison {

		public CompareTo(
				CFG cfg,
				CodeLocation location,
				Expression receiver,
				Expression other) {
			super(cfg, location, "java.lang.Character", JavaCharType.INSTANCE, JavaLibraryFunction.CHARACTER_COMPARE,
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

package it.unive.jlisa.witness.validator.violation;

/**
 * The value fixed by the constraint of a {@code function_return} waypoint
 * ({@code ext_java_expression}), from any tool: {@code \result == <literal>},
 * where the literal is a Java literal (e.g. {@code 5}, {@code -3}, {@code 11L},
 * {@code 0.5f}, {@code 1.0E9}, {@code 'B'}, {@code '\n'}, {@code true}) or
 * {@code Float}/{@code Double} {@code NaN}, {@code POSITIVE_INFINITY} or
 * {@code NEGATIVE_INFINITY}; {@code \result != \result} (NaN); and
 * {@code \result.equals("abc")}. The operands of {@code ==} may be swapped and
 * may be in parentheses. Any other constraint (a range, a conjunction) fixes no
 * single value.
 */
public final class ReturnConstraint {

	private static final String RESULT = "\\result";

	private ReturnConstraint() {
	}

	/**
	 * Returns the value fixed by the constraint: a {@code Boolean},
	 * {@code Long} (integer literals, of any integer type), {@code Float}
	 * ({@code f} literals), {@code Double}, {@code Character} or
	 * {@code String}; null if it fixes no single value.
	 *
	 * @param constraint the value of the constraint
	 */
	public static Object value(
			String constraint) {
		if (constraint == null) {
			return null;
		}
		String c = strip(constraint.trim());
		if (c.replace(" ", "").equals(RESULT + "!=" + RESULT)) {
			return Double.NaN;
		}
		String equals = RESULT + ".equals(";
		if (c.startsWith(equals) && c.endsWith(")")) {
			String arg = c.substring(equals.length(), c.length() - 1).trim();
			return arg.length() >= 2 && arg.startsWith("\"") && arg.endsWith("\"")
					? unescape(arg.substring(1, arg.length() - 1), '"')
					: null;
		}
		int eq = c.indexOf("==");
		if (eq < 0 || c.indexOf("==", eq + 2) >= 0) {
			return null;
		}
		String left = strip(c.substring(0, eq).trim());
		String right = strip(c.substring(eq + 2).trim());
		if (left.equals(RESULT)) {
			return literal(right);
		}
		if (right.equals(RESULT)) {
			return literal(left);
		}
		return null;
	}

	/**
	 * Returns the value of a Java literal, or null if it is not one.
	 */
	static Object literal(
			String text) {
		String t = strip(text);
		switch (t) {
		case "true":
			return Boolean.TRUE;
		case "false":
			return Boolean.FALSE;
		case "Float.NaN":
			return Float.NaN;
		case "Double.NaN":
			return Double.NaN;
		case "Float.POSITIVE_INFINITY":
			return Float.POSITIVE_INFINITY;
		case "Float.NEGATIVE_INFINITY":
			return Float.NEGATIVE_INFINITY;
		case "Double.POSITIVE_INFINITY":
			return Double.POSITIVE_INFINITY;
		case "Double.NEGATIVE_INFINITY":
			return Double.NEGATIVE_INFINITY;
		default:
			break;
		}
		if (t.length() >= 3 && t.startsWith("'") && t.endsWith("'")) {
			String s = unescape(t.substring(1, t.length() - 1), '\'');
			return s != null && s.length() == 1 ? s.charAt(0) : null;
		}
		if (t.startsWith("-") && t.length() > 1) {
			Object v = literal(t.substring(1));
			return switch (v) {
			case Long l -> -l;
			case Float f -> -f;
			case Double d -> -d;
			case null, default -> null;
			};
		}
		try {
			char last = Character.toLowerCase(t.charAt(t.length() - 1));
			if (last == 'l') {
				return Long.decode(t.substring(0, t.length() - 1).replace("_", ""));
			}
			if (last == 'f') {
				return Float.parseFloat(t);
			}
			if (last == 'd' || t.contains(".") || (!t.startsWith("0x") && t.toLowerCase().contains("e"))) {
				return Double.parseDouble(t);
			}
			return Long.decode(t.replace("_", ""));
		} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
			return null;
		}
	}

	/** Removes the parentheses around the whole text. */
	private static String strip(
			String text) {
		String t = text;
		while (t.length() >= 2 && t.startsWith("(") && t.endsWith(")") && balanced(t.substring(1, t.length() - 1))) {
			t = t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	private static boolean balanced(
			String text) {
		int depth = 0;
		for (char ch : text.toCharArray()) {
			if (ch == '(') {
				depth++;
			} else if (ch == ')' && --depth < 0) {
				return false;
			}
		}
		return depth == 0;
	}

	/**
	 * Returns the text of a Java literal body delimited by {@code quote}, or
	 * null if an escape is wrong or the quote appears unescaped.
	 */
	static String unescape(
			String body,
			char quote) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < body.length(); i++) {
			char ch = body.charAt(i);
			if (ch == quote) {
				return null;
			}
			if (ch != '\\') {
				out.append(ch);
				continue;
			}
			if (++i >= body.length()) {
				return null;
			}
			char e = body.charAt(i);
			switch (e) {
			case 'b' -> out.append('\b');
			case 't' -> out.append('\t');
			case 'n' -> out.append('\n');
			case 'f' -> out.append('\f');
			case 'r' -> out.append('\r');
			case 's' -> out.append(' ');
			case '"', '\'', '\\' -> out.append(e);
			case 'u' -> {
				while (i + 1 < body.length() && body.charAt(i + 1) == 'u') {
					i++;
				}
				if (i + 4 >= body.length()) {
					return null;
				}
				try {
					out.append((char) Integer.parseInt(body.substring(i + 1, i + 5), 16));
				} catch (NumberFormatException | StringIndexOutOfBoundsException x) {
					return null;
				}
				i += 4;
			}
			default -> {
				if (e < '0' || e > '7') {
					return null;
				}
				int end = i;
				int max = e <= '3' ? 3 : 2;
				while (end + 1 < body.length() && end + 1 - i < max && body.charAt(end + 1) >= '0'
						&& body.charAt(end + 1) <= '7') {
					end++;
				}
				out.append((char) Integer.parseInt(body.substring(i, end + 1), 8));
				i = end;
			}
			}
		}
		return out.toString();
	}
}

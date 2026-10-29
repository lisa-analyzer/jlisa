package it.unive.jlisa.witness.validator.search;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import java.util.List;

/**
 * A value the input search returns from a {@code Verifier.nondet*()} call or
 * reads from the program: a number (chars and booleans included, as their
 * numeric value) or a string.
 */
public record Val(
		Double num,
		String str) {

	public static Val of(
			double d) {
		return new Val(d, null);
	}

	public static Val of(
			String s) {
		return new Val(null, s);
	}

	/**
	 * Same value, comparing numbers numerically (an int 0 and a double 0.0 are
	 * the same).
	 */
	public boolean same(
			Val o) {
		if (num != null && o.num != null) {
			return num.doubleValue() == o.num.doubleValue();
		}
		return str != null && str.equals(o.str);
	}

	@Override
	public String toString() {
		return num != null ? format(num) : "\"" + str + "\"";
	}

	static String format(
			double d) {
		return d == Math.rint(d) && Math.abs(d) < 1e18 ? Long.toString((long) d) : Double.toString(d);
	}

	/** A literal of the program text (number, char, string, true / false). */
	static Val literal(
			String token) {
		if (token.startsWith("\"")) {
			return of(unescape(token.substring(1, token.length() - 1)));
		}
		if (token.startsWith("'")) {
			String c = unescape(token.substring(1, token.length() - 1));
			return of(c.isEmpty() ? 0 : c.charAt(0));
		}
		if (token.equals("true")) {
			return of(1);
		}
		if (token.equals("false")) {
			return of(0);
		}
		return of(Double.parseDouble(token.replaceAll("[lLfFdD]$", "")));
	}

	private static String unescape(
			String s) {
		return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\'", "'")
				.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	/**
	 * The value of a JDI value, if it is a number, char, boolean, boxed
	 * primitive or string.
	 */
	static Val read(
			Value v) {
		if (v == null) {
			return null;
		}
		if (v instanceof BooleanValue b) {
			return of(b.value() ? 1 : 0);
		}
		if (v instanceof CharValue c) {
			return of(c.value());
		}
		if (v instanceof PrimitiveValue p) {
			return of(p.doubleValue());
		}
		if (v instanceof StringReference s) {
			return of(s.value());
		}
		if (v instanceof ObjectReference o && !(o instanceof ArrayReference)
				&& o.referenceType().name().startsWith("java.lang.")) {
			Field f = o.referenceType().fieldByName("value");
			if (f != null && o.getValue(f) instanceof PrimitiveValue inner) {
				return read(inner);
			}
		}
		return null;
	}

	/**
	 * The JDI mirror of this value for a nondet method's return type, or null
	 * for other types.
	 */
	Value mirror(
			VirtualMachine vm,
			String type) {
		double d = num != null ? num : 0;
		return switch (type) {
		case "boolean" -> vm.mirrorOf(num != null ? d != 0 : str != null && !str.isEmpty());
		case "byte" -> vm.mirrorOf((byte) clamp(d, Byte.MIN_VALUE, Byte.MAX_VALUE));
		case "short" -> vm.mirrorOf((short) clamp(d, Short.MIN_VALUE, Short.MAX_VALUE));
		case "char" -> vm.mirrorOf((char) clamp(d, Character.MIN_VALUE, Character.MAX_VALUE));
		case "int" -> vm.mirrorOf((int) clamp(d, Integer.MIN_VALUE, Integer.MAX_VALUE));
		case "long" -> vm.mirrorOf((long) clamp(d, Long.MIN_VALUE, Long.MAX_VALUE));
		case "float" -> vm.mirrorOf((float) d);
		case "double" -> vm.mirrorOf(d);
		case "java.lang.String" -> vm.mirrorOf(str != null ? str : format(d));
		default -> null;
		};
	}

	private static double clamp(
			double d,
			double lo,
			double hi) {
		return Math.max(lo, Math.min(hi, d));
	}

	/**
	 * The value used when nothing else is known: 0, false or the empty string.
	 */
	static Val defaultFor(
			String type) {
		return type.equals("java.lang.String") ? of("") : of(0);
	}

	/**
	 * The k-th "color" of a type: the colored run gives every call site its own
	 * recognizable value, so that a variable holding it can be traced back to
	 * the site.
	 */
	static Val color(
			String type,
			int k) {
		return switch (type) {
		case "boolean" -> of(k % 2);
		case "byte" -> of(37 + k);
		case "char" -> of('k' + k);
		case "java.lang.String" -> of("c" + (7001 + k));
		case "float", "double" -> of(70.125 + k);
		default -> of(7001 + k);
		};
	}

	/** Boundary values of a type, tried when nothing was learned. */
	static List<Val> boundaries(
			String type) {
		return switch (type) {
		case "boolean" -> List.of(of(1), of(0));
		case "byte" -> List.of(of(1), of(-1), of(Byte.MIN_VALUE), of(Byte.MAX_VALUE));
		case "short" -> List.of(of(1), of(-1), of(Short.MIN_VALUE), of(Short.MAX_VALUE));
		case "char" -> List.of(of('a'), of('A'), of('0'), of(' '));
		case "int" -> List.of(of(1), of(-1), of(Integer.MIN_VALUE), of(Integer.MAX_VALUE));
		case "long" -> List.of(of(1), of(-1), of(Long.MIN_VALUE), of(Long.MAX_VALUE));
		case "float", "double" -> List.of(of(1), of(-1), of(0.5), of(-1e9), of(1e9));
		case "java.lang.String" -> List.of(of("a"), of(" "), of("0"));
		default -> List.of();
		};
	}
}

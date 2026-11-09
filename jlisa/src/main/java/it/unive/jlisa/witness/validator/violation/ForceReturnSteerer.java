package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import it.unive.jlisa.witness.validator.ValidationException;

/**
 * Injects return values into {@code Verifier.nondet*()} methods using
 * {@link ThreadReference#forceEarlyReturn(Value)}.
 * <p>
 * This is the primary steering mechanism for violation witnesses. When the
 * target JVM enters a {@code Verifier.nondet*} method we intercept it at the
 * {@code MethodEntryEvent} level and force it to return the witness-specified
 * value <em>without executing the method body</em>. All JVM semantics (return
 * type checking, stack unwinding) remain intact; only the return value is
 * determined by the witness.
 * <h3>Return-type mapping</h3>
 * <p>
 * The JVM method descriptor (e.g. {@code ()I}, {@code ()Z}) is used to select
 * the correct {@link VirtualMachine#mirrorOf} overload, ensuring the
 * {@link Value} mirror has the exact type the JVM expects.
 */
public final class ForceReturnSteerer {

	private ForceReturnSteerer() {
	}

	/**
	 * Forces the currently-entered method to return {@code value} as a
	 * primitive or {@code String}.
	 *
	 * @param thread     the suspended thread at the {@code MethodEntryEvent}
	 * @param value      the concrete value string from the witness (e.g.
	 *                       {@code "0"}, {@code "true"})
	 * @param descriptor the JVM method descriptor (e.g. {@code ()I} for
	 *                       {@code int nondetInt()})
	 * 
	 * @throws ValidationException if the value cannot be mirrored or the early
	 *                                 return fails
	 */
	public static void steer(
			ThreadReference thread,
			String value,
			String descriptor)
			throws ValidationException {

		VirtualMachine vm = thread.virtualMachine();
		Value mirror = mirrorValue(vm, value, returnTypeDescriptor(descriptor));

		try {
			thread.forceEarlyReturn(mirror);
		} catch (Exception e) {
			throw new ValidationException(
					"forceEarlyReturn failed for value='" + value + "' descriptor='" + descriptor
							+ "': " + e.getMessage(),
					e);
		}
	}

	/**
	 * Forces the currently-entered method to return the value of a witness
	 * constraint (see {@link ReturnConstraint#value}), converted to the return
	 * type of the method: an integer literal fits any integer type or
	 * {@code char} when in range, a number any floating-point type.
	 *
	 * @param thread     the suspended thread at the {@code MethodEntryEvent}
	 * @param value      the value
	 * @param descriptor the JVM method descriptor
	 * 
	 * @throws ValidationException if the value does not fit the return type or
	 *                                 the early return fails
	 */
	public static void steer(
			ThreadReference thread,
			Object value,
			String descriptor)
			throws ValidationException {
		Value mirror = mirrorTyped(thread.virtualMachine(), value, returnTypeDescriptor(descriptor));
		try {
			thread.forceEarlyReturn(mirror);
		} catch (Exception e) {
			throw new ValidationException(
					"forceEarlyReturn failed for value=" + value + " descriptor='" + descriptor + "': "
							+ e.getMessage(),
					e);
		}
	}

	private static Value mirrorTyped(
			VirtualMachine vm,
			Object value,
			String retType)
			throws ValidationException {
		Value mirror = switch (retType) {
		case "Z" -> value instanceof Boolean b ? vm.mirrorOf(b) : null;
		case "I" -> integral(value, Integer.MIN_VALUE, Integer.MAX_VALUE) instanceof Long l ? vm.mirrorOf(l.intValue())
				: null;
		case "J" -> integral(value, Long.MIN_VALUE, Long.MAX_VALUE) instanceof Long l ? vm.mirrorOf(l) : null;
		case "S" -> integral(value, Short.MIN_VALUE, Short.MAX_VALUE) instanceof Long l
				? vm.mirrorOf(l.shortValue())
				: null;
		case "B" -> integral(value, Byte.MIN_VALUE, Byte.MAX_VALUE) instanceof Long l ? vm.mirrorOf(l.byteValue())
				: null;
		case "C" -> value instanceof Character c ? vm.mirrorOf(c)
				: integral(value, Character.MIN_VALUE, Character.MAX_VALUE) instanceof Long l
						? vm.mirrorOf((char) l.longValue())
						: null;
		case "F" -> value instanceof Number n && !(value instanceof Double d && !d.isNaN() && !d.isInfinite()
				&& (double) d.floatValue() != d) ? vm.mirrorOf(n.floatValue()) : null;
		case "D" -> value instanceof Number n ? vm.mirrorOf(n.doubleValue()) : null;
		case "Ljava/lang/String;" -> value instanceof String s ? vm.mirrorOf(s) : null;
		default -> null;
		};
		if (mirror == null) {
			throw new ValidationException("The value " + value + " of the witness does not fit the return type '"
					+ retType + "'");
		}
		return mirror;
	}

	/**
	 * Returns the value as a Long if it is an integer in the range, else null.
	 */
	private static Long integral(
			Object value,
			long min,
			long max) {
		long v;
		if (value instanceof Long l) {
			v = l;
		} else if (value instanceof Character c) {
			v = c;
		} else {
			return null;
		}
		return v < min || v > max ? null : v;
	}

	// ------------------------------------------------------------------
	// private helpers
	// ------------------------------------------------------------------

	/**
	 * Extracts the return type character/string from a full method descriptor.
	 * E.g. {@code "()I"} → {@code "I"}, {@code "([II)Z"} → {@code "Z"},
	 * {@code "()Ljava/lang/String;"} → {@code "Ljava/lang/String;"}.
	 */
	private static String returnTypeDescriptor(
			String descriptor) {
		if (descriptor == null || descriptor.isBlank()) {
			return "V"; // void — should not happen for nondet methods
		}
		int closeParenIdx = descriptor.lastIndexOf(')');
		if (closeParenIdx < 0 || closeParenIdx >= descriptor.length() - 1) {
			return "V";
		}
		return descriptor.substring(closeParenIdx + 1);
	}

	/**
	 * Creates a JDI {@link Value} mirror for the given string representation
	 * and return type descriptor character.
	 */
	private static Value mirrorValue(
			VirtualMachine vm,
			String value,
			String retType)
			throws ValidationException {
		try {
			return switch (retType) {
			case "Z" -> vm.mirrorOf(Boolean.parseBoolean(value));
			case "I" -> vm.mirrorOf(Integer.parseInt(value));
			case "J" -> vm.mirrorOf(Long.parseLong(value));
			case "F" -> vm.mirrorOf(Float.parseFloat(value));
			case "D" -> vm.mirrorOf(Double.parseDouble(value));
			case "B" -> vm.mirrorOf(Byte.parseByte(value));
			case "S" -> vm.mirrorOf(Short.parseShort(value));
			case "C" -> {
				// value may be a char literal like 'a' or a raw character
				String stripped = value.replaceAll("^'|'$", "");
				yield vm.mirrorOf(stripped.isEmpty() ? '\0' : stripped.charAt(0));
			}
			case "Ljava/lang/String;" -> vm.mirrorOf(unquote(value));
			case "V" -> null; // void return — forceEarlyReturn(null) is valid
								// for void methods
			default -> throw new ValidationException(
					"Unsupported return type '" + retType + "' for value '" + value + "'");
			};
		} catch (NumberFormatException e) {
			throw new ValidationException(
					"Cannot parse '" + value + "' as " + retType + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Strips surrounding double-quotes from a string literal, if present.
	 */
	private static String unquote(
			String s) {
		if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
			return s.substring(1, s.length() - 1);
		}
		return s;
	}
}

package it.unive.jlisa.witness.validator.violation;

import com.sun.jdi.*;
import it.unive.jlisa.witness.validator.ValidationException;

import java.util.List;

/**
 * Sets local variable values on a suspended thread at a breakpoint, implementing the
 * {@link StackFrame#setValue} steering path.
 *
 * <p>Used for:
 * <ul>
 *   <li>v2 {@code assumption} waypoints that provide an explicit variable value
 *       (e.g. {@code "x == 101"} at a specific source line)</li>
 *   <li>v2 {@code branching} waypoints where the branch condition must be flipped to
 *       follow the witness path</li>
 * </ul>
 */
public final class VariableSteerer {

    private VariableSteerer() {
    }

    /**
     * Attempts to set local variable {@code varName} to {@code value} in the top frame of
     * the suspended {@code thread}.
     *
     * <p>If the variable is not visible in the current frame the call is a no-op with a
     * warning — this is a graceful degradation for witnesses that reference variables
     * outside their scope.
     *
     * @param thread  the suspended thread (at a {@code BreakpointEvent})
     * @param varName the local variable name to set
     * @param value   the string representation of the target value (e.g. {@code "101"})
     * @throws ValidationException if setting the value fails for a recoverable reason
     */
    public static void setValue(ThreadReference thread, String varName, String value)
            throws ValidationException {

        StackFrame frame;
        try {
            frame = thread.frame(0);
        } catch (Exception e) {
            ValidatorLogger.warn("[STEER] Cannot access top frame: {}", e.getMessage());
            return;
        }

        LocalVariable variable = findVariable(frame, varName);
        if (variable == null) {
            ValidatorLogger.warn("[STEER] Variable '{}' not visible in current frame — skipped", varName);
            return;
        }

        VirtualMachine vm = thread.virtualMachine();
        Value mirror = mirrorFromType(vm, frame, variable, value);
        if (mirror == null) {
            ValidatorLogger.warn("[STEER] Cannot mirror value '{}' for variable '{}' — skipped",
                    value, varName);
            return;
        }

        try {
            frame.setValue(variable, mirror);
            ValidatorLogger.event("[STEER] Set {} = {}", varName, value);
        } catch (InvalidTypeException | ClassNotLoadedException e) {
            throw new ValidationException(
                    "Failed to set variable '" + varName + "' = '" + value + "': " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private static LocalVariable findVariable(StackFrame frame, String varName) {
        try {
            List<LocalVariable> visibleVars = frame.visibleVariables();
            for (LocalVariable v : visibleVars) {
                if (v.name().equals(varName)) {
                    return v;
                }
            }
        } catch (Exception e) {
            ValidatorLogger.debug("[STEER] Cannot list visible variables: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Creates a JDI mirror whose static type matches the existing variable.
     * We infer the intended type from the variable's current value type, falling back to
     * common numeric types based on parsing.
     */
    private static Value mirrorFromType(
            VirtualMachine vm, StackFrame frame, LocalVariable variable, String value) {
        try {
            // Inspect the current value to learn the type
            Value current = frame.getValue(variable);
            if (current instanceof BooleanValue) {
                return vm.mirrorOf(Boolean.parseBoolean(value));
            }
            if (current instanceof IntegerValue) {
                return vm.mirrorOf(Integer.parseInt(value));
            }
            if (current instanceof LongValue) {
                return vm.mirrorOf(Long.parseLong(value));
            }
            if (current instanceof FloatValue) {
                return vm.mirrorOf(Float.parseFloat(value));
            }
            if (current instanceof DoubleValue) {
                return vm.mirrorOf(Double.parseDouble(value));
            }
            if (current instanceof ByteValue) {
                return vm.mirrorOf(Byte.parseByte(value));
            }
            if (current instanceof ShortValue) {
                return vm.mirrorOf(Short.parseShort(value));
            }
            if (current instanceof CharValue) {
                String stripped = value.replaceAll("^'|'$", "");
                return vm.mirrorOf(stripped.isEmpty() ? '\0' : stripped.charAt(0));
            }
        } catch (Exception ignored) {
            // Fallback: try common numeric types
        }

        // Fallback heuristics
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return vm.mirrorOf(Boolean.parseBoolean(value));
        }
        try {
            return vm.mirrorOf(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }
        try {
            return vm.mirrorOf(Double.parseDouble(value));
        } catch (NumberFormatException ignored) {
        }

        return null;
    }
}

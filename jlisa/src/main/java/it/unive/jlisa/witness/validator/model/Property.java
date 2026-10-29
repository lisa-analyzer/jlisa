package it.unive.jlisa.witness.validator.model;

import it.unive.jlisa.witness.validator.ValidationException;

/**
 * The property a violation witness is validated against, as passed by name on the command line.
 *
 * <p>The SV-COMP wrapper parses the {@code .prp} file and passes the name; the names are the
 * same as in JLiSA's SV-COMP wrapper.
 */
public enum Property {

    /** {@code valid-assert.prp}: no {@code assert} statement fails. */
    ASSERT("assert"),

    /** {@code no-runtime-exception.prp}: no uncaught {@code java.lang.RuntimeException}. */
    RUNTIME("runtime");

    private final String name;

    Property(String name) {
        this.name = name;
    }

    /**
     * Returns the property with the given command-line name.
     *
     * @throws ValidationException if the name is not a supported property
     */
    public static Property fromName(String name) throws ValidationException {
        for (Property p : values()) {
            if (p.name.equals(name)) {
                return p;
            }
        }
        throw new ValidationException("Unsupported property: " + name);
    }

    @Override
    public String toString() {
        return name;
    }
}

package it.unive.jlisa.svcomp;

import it.unive.jlisa.checkers.ViolationKind;

/**
 * The properties of the Java tasks of SV-COMP, with the text of their property
 * files.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public enum SVCOMPProperty {

	/**
	 * No assertion fails ({@code valid-assert.prp}).
	 */
	ASSERT("valid-assert", "CHECK( init(Main.main()), LTL(G assert) )", ViolationKind.ASSERT),

	/**
	 * No runtime exception is left uncaught ({@code no-runtime-exception.prp}).
	 */
	RUNTIME_EXCEPTION("no-runtime-exception",
			"CHECK(init(Main.main()), LTL(G ! uncaught(java.lang.RuntimeException)))",
			ViolationKind.RUNTIME_EXCEPTION);

	/**
	 * The name of this property.
	 */
	private final String name;

	/**
	 * The text of the property file, without leading and trailing blanks.
	 */
	private final String specification;

	/**
	 * The kind of the violations of this property.
	 */
	private final ViolationKind violation;

	/**
	 * Builds a property.
	 *
	 * @param name          the name of the property
	 * @param specification the text of the property file
	 * @param violation     the kind of the violations of the property
	 */
	private SVCOMPProperty(
			String name,
			String specification,
			ViolationKind violation) {
		this.name = name;
		this.specification = specification;
		this.violation = violation;
	}

	/**
	 * Returns the name of this property.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the text of the property file of this property, without leading
	 * and trailing blanks.
	 *
	 * @return the text
	 */
	public String getSpecification() {
		return specification;
	}

	/**
	 * Returns the kind of the violations of this property.
	 *
	 * @return the kind
	 */
	public ViolationKind getViolation() {
		return violation;
	}

	/**
	 * Returns the property whose property file has the given text. Leading and
	 * trailing blanks are ignored.
	 *
	 * @param specification the text of a property file
	 *
	 * @return the property, or {@code null} if no property has that text
	 */
	public static SVCOMPProperty of(
			String specification) {
		String text = specification.strip();
		for (SVCOMPProperty property : values())
			if (property.specification.equals(text))
				return property;
		return null;
	}
}

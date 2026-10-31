package it.unive.jlisa;

/**
 * The version of jlisa.
 */
public final class Version {

	/**
	 * The system property with the version of jlisa when it does not run from
	 * its jar (e.g., from the Gradle tasks, which run the compiled classes).
	 */
	public static final String PROPERTY = "jlisa.version";

	private Version() {
	}

	/**
	 * Returns the version of jlisa: the one in the manifest of its jar, else
	 * the one of the system property {@value #PROPERTY}.
	 *
	 * @return the version, or {@code null} if neither gives one
	 */
	public static String get() {
		String version = Version.class.getPackage().getImplementationVersion();
		return version != null ? version : System.getProperty(PROPERTY);
	}
}

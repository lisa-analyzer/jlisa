package it.unive.jlisa.analysis.value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Optional bounds on the native memory of Apron, off unless the environment
 * variables below are set. Apron frees a native state only when the garbage
 * collector reclaims the small Java object that wraps it: with a large heap the
 * collector runs rarely, and the native states pile up. Every
 * {@link #CHECK_EVERY} operations, the resident memory of the process minus the
 * memory of the heap is read (it also counts the memory of the JVM outside the
 * heap, about 0.5-1 GB):
 * <ul>
 * <li>above {@code JLISA_APRON_GC_MB} MB, a collection is requested;</li>
 * <li>above {@code JLISA_APRON_GIVE_UP_MB} MB, after the collection, Apron is
 * given up for the rest of the analysis (see {@link #exhausted()}): the states
 * kept by the analysis are alive, and no collection can free them.</li>
 * </ul>
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class NativeMemory {

	private static final int CHECK_EVERY = 500;

	private static final long GC_ABOVE = megabytes("JLISA_APRON_GC_MB");

	private static final long GIVE_UP_ABOVE = megabytes("JLISA_APRON_GIVE_UP_MB");

	private static volatile boolean exhausted = false;

	private static final long PAGE = 4096;

	private static final Path STATM = Path.of("/proc/self/statm");

	private static int operations = 0;

	private NativeMemory() {
	}

	// the bytes given in MB by the environment variable, or 0 (off)
	private static long megabytes(
			String variable) {
		try {
			String value = System.getenv(variable);
			return value == null ? 0 : Long.parseLong(value.trim()) << 20;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Counts an operation of Apron, requesting a collection or giving up Apron
	 * when the native memory grows beyond the limits that are set.
	 */
	public static synchronized void operation() {
		if (GC_ABOVE <= 0 && GIVE_UP_ABOVE <= 0)
			return;
		if (++operations < CHECK_EVERY)
			return;
		operations = 0;
		long resident = resident();
		if (resident < 0)
			return;
		long nativeMemory = resident - Runtime.getRuntime().totalMemory();
		if (GC_ABOVE > 0 && nativeMemory > GC_ABOVE) {
			System.gc();
			resident = resident();
			if (resident < 0)
				return;
			nativeMemory = resident - Runtime.getRuntime().totalMemory();
		}
		if (GIVE_UP_ABOVE > 0 && nativeMemory > GIVE_UP_ABOVE)
			exhausted = true;
	}

	/**
	 * Returns whether the native memory of Apron grew beyond the limit set by
	 * {@code JLISA_APRON_GIVE_UP_MB}: from then on, Apron is not used.
	 *
	 * @return whether Apron is given up
	 */
	public static boolean exhausted() {
		return exhausted;
	}

	// the resident memory of the process in bytes, or -1 if unknown
	private static long resident() {
		try {
			String[] fields = Files.readString(STATM).trim().split("\\s+");
			return Long.parseLong(fields[1]) * PAGE;
		} catch (IOException | RuntimeException e) {
			return -1;
		}
	}
}

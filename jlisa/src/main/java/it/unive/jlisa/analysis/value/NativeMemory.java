package it.unive.jlisa.analysis.value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps the native memory of Apron bounded. Apron frees a native state only
 * when the garbage collector reclaims the small Java object that wraps it: with
 * a large heap the collector runs rarely, and the native states pile up (tens
 * of GB). Every {@link #CHECK_EVERY} operations, the resident memory of the
 * process is read from {@code /proc/self/statm}, and a collection is requested
 * when it exceeds the memory used by the heap by more than {@link #MAX_NATIVE}
 * bytes. If it still exceeds it by more than {@link #EXHAUSTED} bytes, Apron is
 * given up for the rest of the analysis (see {@link #exhausted()}): the states
 * kept by the analysis are alive, and no collection can free them.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class NativeMemory {

	private static final int CHECK_EVERY = 500;

	private static final long MAX_NATIVE = 1L << 30;

	private static final long EXHAUSTED = 2L << 30;

	private static volatile boolean exhausted = false;

	private static final long PAGE = 4096;

	private static final Path STATM = Path.of("/proc/self/statm");

	private static int operations = 0;

	private NativeMemory() {
	}

	/**
	 * Counts an operation of Apron, requesting a collection when the native
	 * memory grows too much.
	 */
	public static synchronized void operation() {
		if (++operations < CHECK_EVERY)
			return;
		operations = 0;
		long resident = resident();
		if (resident < 0)
			return;
		long heap = Runtime.getRuntime().totalMemory();
		if (resident - heap > MAX_NATIVE) {
			System.gc();
			resident = resident();
			heap = Runtime.getRuntime().totalMemory();
			if (resident >= 0 && resident - heap > EXHAUSTED)
				exhausted = true;
		}
	}

	/**
	 * Returns whether the native memory of Apron grew beyond the limit, even
	 * after a collection: from then on, Apron is not used.
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

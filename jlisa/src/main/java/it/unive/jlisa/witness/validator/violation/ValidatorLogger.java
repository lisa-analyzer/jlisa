package it.unive.jlisa.witness.validator.violation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Structured logging helper for the witness validator.
 * <p>
 * All output goes through Log4j2. Log lines are prefixed with bracketed tags to
 * allow easy grepping during interactive debugging (e.g.
 * {@code grep '\[JDI\]'}).
 * <p>
 * The log level is controlled externally: {@code -l}/{@code --log-level} sets
 * it (e.g. {@code -l ERROR}, as the SV-COMP wrapper does to keep the output of
 * BenchExec runs small).
 */
public final class ValidatorLogger {

	private static final Logger LOG = LogManager.getLogger(ValidatorLogger.class);

	private ValidatorLogger() {
	}

	public static void parse(
			String fmt,
			Object... args) {
		LOG.info("[PARSE]  " + fmt, args);
	}

	public static void filter(
			String fmt,
			Object... args) {
		LOG.info("[FILTER] " + fmt, args);
	}

	public static void plan(
			String fmt,
			Object... args) {
		LOG.info("[PLAN]   " + fmt, args);
	}

	public static void jdi(
			String fmt,
			Object... args) {
		LOG.info("[JDI]    " + fmt, args);
	}

	public static void event(
			String fmt,
			Object... args) {
		LOG.info("[EVENT]  " + fmt, args);
	}

	public static void result(
			String fmt,
			Object... args) {
		LOG.info("[RESULT] " + fmt, args);
	}

	public static void warn(
			String fmt,
			Object... args) {
		LOG.warn(fmt, args);
	}

	public static void debug(
			String fmt,
			Object... args) {
		LOG.debug(fmt, args);
	}

	public static void error(
			String fmt,
			Object... args) {
		LOG.error(fmt, args);
	}
}

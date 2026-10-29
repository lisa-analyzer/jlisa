package it.unive.jlisa.witness.validator.violation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Structured logging helper for the witness validator.
 *
 * <p>All output goes to {@code stderr} via Log4j2, leaving {@code stdout} available exclusively
 * for the BenchExec verdict line.  Log lines are prefixed with bracketed tags to allow
 * easy grepping during interactive debugging (e.g. {@code grep '\[JDI\]'}).
 *
 * <p>The log level is controlled externally: {@code Main} sets it to {@code DEBUG} when
 * {@code --verbose} is passed, and leaves it at {@code WARN} otherwise so that BenchExec
 * runs produce minimal stderr noise.
 */
public final class ValidatorLogger {

    private static final Logger LOG = LogManager.getLogger(ValidatorLogger.class);

    private ValidatorLogger() {
    }

    public static void parse(String fmt, Object... args) {
        LOG.info("[PARSE]  " + fmt, args);
    }

    public static void filter(String fmt, Object... args) {
        LOG.info("[FILTER] " + fmt, args);
    }

    public static void plan(String fmt, Object... args) {
        LOG.info("[PLAN]   " + fmt, args);
    }

    public static void jdi(String fmt, Object... args) {
        LOG.info("[JDI]    " + fmt, args);
    }

    public static void event(String fmt, Object... args) {
        LOG.info("[EVENT]  " + fmt, args);
    }

    public static void result(String fmt, Object... args) {
        LOG.info("[RESULT] " + fmt, args);
    }

    public static void warn(String fmt, Object... args) {
        LOG.warn(fmt, args);
    }

    public static void debug(String fmt, Object... args) {
        LOG.debug(fmt, args);
    }

    public static void error(String fmt, Object... args) {
        LOG.error(fmt, args);
    }
}

package it.unive.jlisa.witness.validator;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * The logging configuration of the validator, set in code.
 *
 * <p>The validator is an application, so it decides its own configuration. A {@code log4j2.xml} in
 * jlisa-wit resources would clash with the one in jlisa's jar (a dependency): Log4j loads only one of the
 * two, depending on the classpath order. Therefore, this class sets the configuration programmatically.
 *
 * <p>All output goes to stderr, except for the verdict line which is printed to stdout. The default level
 * is WARN, raised to DEBUG by {@link Main} when {@code --verbose} is given.
 */
public final class Logging {

    private Logging() {
    }

    /** Applies the validator's logging configuration. Should be called first in {@code main}. */
    public static void configure() {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.WARN);
        builder.setConfigurationName("jlisa-witness-validator");

        AppenderComponentBuilder stderr = builder.newAppender("StdErr", "Console")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR);
        stderr.add(builder.newLayout("PatternLayout").addAttribute("pattern", "%-5level %msg%n"));
        builder.add(stderr);
        builder.add(builder.newRootLogger(Level.WARN).add(builder.newAppenderRef("StdErr")));

        Configurator.reconfigure(builder.build());
    }
}

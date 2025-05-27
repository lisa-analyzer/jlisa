package it.unive.jlisa;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.osgi.service.log.LogLevel;


public class LogManager {
    private static final String LISA_LOGGER = "it.unive.lisa";
    private static final String JLISA_LOGGER = "it.unive.jlisa";
    public static void setLogLevel(Level level) {
        Configurator.setLevel(LISA_LOGGER, level);
        Configurator.setLevel(JLISA_LOGGER, level);
    }


}

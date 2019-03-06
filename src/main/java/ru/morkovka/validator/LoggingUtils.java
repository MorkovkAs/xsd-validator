package ru.morkovka.validator;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.LogManager;
import java.util.logging.Logger;

class LoggingUtils {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    /**
     * Create logger
     */
    static Logger createLogger() {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
            return Logger.getLogger(Main.class.getName());
        } catch (IOException e) {
            System.err.println("Could not setup logger configuration: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Format log.
     *
     * @return String for log with general processing info
     */
    static String getFullLog(String text) {
        return String.format("%-12s\t%s", TIME_FORMATTER.format(LocalTime.now()), text);
    }
}

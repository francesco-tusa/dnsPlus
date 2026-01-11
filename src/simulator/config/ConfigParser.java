package simulator.config;

import java.util.Properties;
import java.util.logging.Logger;
import utils.CustomLogger;

public class ConfigParser {
    private static final Logger logger = CustomLogger.getLogger(ConfigParser.class.getName());

    private ConfigParser() {
        // Prevent instantiation
    }

    public static int parseInt(Properties props, String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warning(String.format("Config Warning: Invalid integer for '%s' ('%s'). Using default: %d", key, val, defaultValue));
            return defaultValue;
        }
    }

    public static long parseLong(Properties props, String key, long defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            logger.warning(String.format("Config Warning: Invalid long for '%s' ('%s'). Using default: %d", key, val, defaultValue));
            return defaultValue;
        }
    }

    public static double parseDouble(Properties props, String key, double defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            logger.warning(String.format("Config Warning: Invalid double for '%s' ('%s'). Using default: %f", key, val, defaultValue));
            return defaultValue;
        }
    }

    public static boolean parseBoolean(Properties props, String key, boolean defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    public static String parseString(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue).trim();
    }
    
    public static <T extends Enum<T>> T parseEnum(Properties props, String key, Class<T> enumType, T defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Enum.valueOf(enumType, val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning(String.format("Config Warning: Invalid value '%s' for enum %s in key '%s'. Using default: %s", 
                    val, enumType.getSimpleName(), key, defaultValue));
            return defaultValue;
        }
    }
}
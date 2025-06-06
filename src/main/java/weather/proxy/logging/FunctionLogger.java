package weather.proxy.logging;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FunctionLogger {
    private static final Logger LOGGER = Logger.getLogger(FunctionLogger.class.getName());

    public static <T, R> Function<T, R> log(Function<T, R> func, Level level) {
        return input -> {
            long start = System.currentTimeMillis();
            LOGGER.log(level, "Input: " + input);
            try {
                R result = func.apply(input);
                long duration = System.currentTimeMillis() - start;
                LOGGER.log(level, "Output: " + result + " (Duration: " + duration + "ms)");
                return result;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception during function execution: " + e.getMessage(), e);
                throw e;
            }
        };
    }
}


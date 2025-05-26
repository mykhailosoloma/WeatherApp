package weather.util;

import javafx.application.Platform;
import javafx.scene.layout.Region;

import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeoutRunner {

    public static void runWithTimeout(Iterator<String> generator, Region node, int seconds) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            String color = generator.next();
            Platform.runLater(() -> node.setStyle("-fx-background-color: " + color + ";"));
        }, 0, 2, TimeUnit.SECONDS);

        executor.schedule(executor::shutdown, seconds, TimeUnit.SECONDS);
    }
}

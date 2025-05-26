package weather.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import weather.core.service.WeatherService;

import java.time.Duration;
import java.util.List;
import java.util.prefs.Preferences;

public class WeatherAppUI extends Application {
    private WeatherService weatherService;
    private final Preferences prefs = Preferences.userNodeForPackage(WeatherAppUI.class);
    private static final String CITIES_KEY = "saved_cities";

    @Override
    public void start(Stage stage) throws Exception {
        weatherService = new WeatherService(Duration.ofDays(1), 12);

        initializeCityQueue();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/weather/ui/main-view.fxml"));
        VBox root = loader.load();

        MainController controller = loader.getController();
        controller.setWeatherService(weatherService);

        Scene scene = new Scene(root, 400, 400);
        scene.getStylesheets().add(getClass().getResource("/weather/ui/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Погода 🌤️");
        stage.show();

        String csv = prefs.get(CITIES_KEY, "");
        if (!csv.isEmpty()) {
            weatherService.updateWeatherForTopCities();
        }

        stage.setOnCloseRequest(event -> {
            weatherService.shutdown();
            Platform.exit();
        });
    }

    private void initializeCityQueue() {
        String csv = prefs.get(CITIES_KEY, "");
        if (!csv.isEmpty()) {
            List<String> cities = List.of(csv.split(","));
            // Trigger asynchronous weather fetching for all saved cities
            weatherService.getGroupWeatherAsync(cities);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}

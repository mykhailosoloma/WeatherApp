package weather.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import weather.core.model.WeatherData;
import weather.core.service.WeatherService;
import weather.util.ColorGenerator;
import weather.util.TimeoutRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class MainController {

    @FXML private TextField cityField;
    @FXML private Label resultLabel;
    @FXML private VBox root;
    @FXML private ListView<String> cityList;
    @FXML private ToggleButton unitToggle;
    @FXML private Button filterButton;

    private WeatherService service;
    private final Preferences prefs = Preferences.userNodeForPackage(MainController.class);
    private static final String LAST_CITY_KEY = "last_city";
    private static final String CITIES_KEY = "saved_cities";
    private static final String TEMP_UNIT_KEY = "temp_unit";
    private final ObservableList<String> savedCities = FXCollections.observableArrayList();

    // true = Fahrenheit, false = Celsius
    private boolean isFahrenheit = false;
    private static final double HOT_TEMPERATURE_THRESHOLD = 25.0;

    public void setWeatherService(WeatherService service) {
        this.service = service;
    }

    @FXML
    public void initialize() {
        String csv = prefs.get(CITIES_KEY, "");
        if (!csv.isEmpty()) {
            savedCities.addAll(List.of(csv.split(",")));
        }
        cityList.setItems(savedCities);
        String lastCity = prefs.get(LAST_CITY_KEY, "");
        if (!lastCity.isEmpty()) {
            cityField.setText(lastCity);
        }
        isFahrenheit = prefs.getBoolean(TEMP_UNIT_KEY, false);
        updateUnitToggleText();
    }

    @FXML
    private void onToggleUnit() {
        isFahrenheit = !isFahrenheit;
        prefs.putBoolean(TEMP_UNIT_KEY, isFahrenheit);
        updateUnitToggleText();
        if (!resultLabel.getText().isEmpty() && !resultLabel.getText().startsWith("⚠️") && !resultLabel.getText().startsWith("❌")) {
            String city = cityField.getText().trim();
            if (!city.isEmpty()) {
                onGetWeather();
            }
        }
    }

    private void updateUnitToggleText() {
        unitToggle.setText(isFahrenheit ? "°F" : "°C");
        unitToggle.setSelected(isFahrenheit);
        double displayThreshold = convertTemperature(HOT_TEMPERATURE_THRESHOLD);
        filterButton.setText("Тільки >" + displayThreshold + getTemperatureUnit());
    }

    private double convertTemperature(double celsius) {
        return isFahrenheit ? (celsius * 9/5) + 32 : celsius;
    }

    private String getTemperatureUnit() {
        return isFahrenheit ? "°F" : "°C";
    }

    @FXML
    private void onGetWeather() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            resultLabel.setText("⚠️ Введіть назву міста!");
            return;
        }

        prefs.put(LAST_CITY_KEY, city);

        if (!savedCities.contains(city)) {
            savedCities.add(0, city);
            if (savedCities.size() > 5) {
                savedCities.remove(5, savedCities.size());
            }
            prefs.put(CITIES_KEY, String.join(",", savedCities));
        }
        resultLabel.setText("⏳ Завантаження...");
        service.getWeatherAsync(city)
            .thenAccept(data -> {
                Platform.runLater(() -> {
                    if (data == null) {
                        resultLabel.setText("❌ Помилка: Не вдалося отримати дані про погоду");
                        return;
                    }

                    String weatherText = "📍 " + data.city() + ", " + data.country() + "\n" +
                        "🌡 Темп: " + convertTemperature(data.temperature()) + getTemperatureUnit() + 
                        " (відчувається як " + convertTemperature(data.feelsLike()) + getTemperatureUnit() + ")\n" +
                        "☁️ " + data.description() + "\n" +
                        "💧 Вологість: " + data.humidity() + "%\n" +
                        "💨 Вітер: " + data.windSpeed() + " м/с\n" +
                        "🔄 Тиск: " + data.pressure() + " гПа";
                    resultLabel.setText(weatherText);

                    ColorGenerator generator = new ColorGenerator();
                    TimeoutRunner.runWithTimeout(generator, root, 30);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> resultLabel.setText("❌ Помилка: " + e.getMessage()));
                return null;
            });
    }

    @FXML
    private void onCitySelect() {
        String selected = cityList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cityField.setText(selected);
            onGetWeather();
        }
    }

    @FXML
    private void onFilterHotCities() {
        if (savedCities.isEmpty()) {
            resultLabel.setText("⚠️ Список міст порожній.");
            return;
        }

        resultLabel.setText("⏳ Завантаження...");
        service.getGroupWeatherAsync(new ArrayList<>(savedCities))
            .thenAccept(allCities -> {
                if (allCities == null) {
                    Platform.runLater(() -> resultLabel.setText("❌ Помилка: Не вдалося отримати дані про погоду"));
                    return;
                }
                List<WeatherData> hotCities = new ArrayList<>();
                for (WeatherData data : allCities) {
                    if (data != null && data.temperature() > HOT_TEMPERATURE_THRESHOLD) {
                        hotCities.add(data);
                    }
                }
                Platform.runLater(() -> {
                    if (hotCities.isEmpty()) {
                        double displayThreshold = convertTemperature(HOT_TEMPERATURE_THRESHOLD);
                        resultLabel.setText("🌡 Немає міст з температурою > " + displayThreshold + getTemperatureUnit());
                    } else {
                        StringBuilder result = new StringBuilder();
                        for (WeatherData data : hotCities) {
                            if (!result.isEmpty()) {
                                result.append("\n\n");
                            }
                            result.append("📍 ").append(data.city()).append(", ").append(data.country())
                                .append(" — ").append(convertTemperature(data.temperature())).append(getTemperatureUnit())
                                .append(" (відчувається як ").append(convertTemperature(data.feelsLike())).append(getTemperatureUnit()).append(")\n")
                                .append("   💧 ").append(data.humidity()).append("% | 💨 ").append(data.windSpeed()).append(" м/с");
                        }
                        resultLabel.setText(result.toString());
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> resultLabel.setText("❌ Помилка: " + e.getMessage()));
                return null;
            });
    }

    @FXML
    private void onClearCache() {
        service.clearCache();
        resultLabel.setText("Кеш очищено");
    }
}

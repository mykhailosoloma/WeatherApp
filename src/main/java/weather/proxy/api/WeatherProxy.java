package weather.proxy.api;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import weather.core.model.WeatherData;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WeatherProxy {
    private static final String API_KEY = "f308b13bd984ce97bbfd2be4d45c1872";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Integer> cityIdMap = new HashMap<>();

    public WeatherProxy() {
        loadCityList();
    }

    private void loadCityList() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("city.list.json")) {
            if (inputStream == null) {
                throw new RuntimeException("city.list.json not found");
            }

            List<City> cities = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            for (City city : cities) {
                cityIdMap.put(city.getName().toLowerCase(), city.getId());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load city list", e);
        }
    }

    public WeatherData fetchWeather(String cityNameInput) throws Exception {
        Integer cityId = cityIdMap.get(cityNameInput.toLowerCase());
        if (cityId == null) {
            throw new IllegalArgumentException("City not found: " + cityNameInput);
        }

        String url = String.format("%s?id=%d&appid=%s&units=metric", API_URL, cityId, API_KEY);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        return parseWeatherData(objectMapper.readTree(responseBody));
    }

    public CompletableFuture<WeatherData> fetchWeatherAsync(String cityNameInput) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchWeather(cityNameInput);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch weather for " + cityNameInput, e);
            }
        });
    }

    private WeatherData parseWeatherData(JsonNode jsonNode) {
        String cityName = jsonNode.get("name").asText();
        double temperature = jsonNode.get("main").get("temp").asDouble();
        String description = jsonNode.get("weather").get(0).get("description").asText();
        double humidity = jsonNode.get("main").get("humidity").asDouble();
        double windSpeed = jsonNode.get("wind").get("speed").asDouble();
        double pressure = jsonNode.get("main").get("pressure").asDouble();
        double feelsLike = jsonNode.get("main").get("feels_like").asDouble();
        String country = jsonNode.get("sys").get("country").asText();

        return new WeatherData(cityName, temperature, description, 
                              humidity, windSpeed, pressure, feelsLike, country);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class City {
        @com.fasterxml.jackson.annotation.JsonProperty
        private int id;
        @com.fasterxml.jackson.annotation.JsonProperty
        private String name;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

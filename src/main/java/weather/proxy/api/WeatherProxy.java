package weather.proxy.api;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import weather.core.model.WeatherData;
import weather.proxy.auth.ApiKeyAuthStrategy;
import weather.proxy.auth.AuthorizationStrategy;
import weather.proxy.logging.ProxyLogger;
import weather.proxy.ratelimit.RateLimiter;
import weather.util.LargeDataProcessor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Stream;

public class WeatherProxy {
    private static final String DEFAULT_API_KEY = "f308b13bd984ce97bbfd2be4d45c1872";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String API_KEY_PARAM = "appid";
    private static final int DEFAULT_MAX_REQUESTS = 60;
    private static final Duration DEFAULT_RATE_LIMIT_PERIOD = Duration.ofMinutes(1);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Integer> cityIdMap = new HashMap<>();
    private final AuthorizationStrategy authStrategy;
    private final ProxyLogger logger;
    private final RateLimiter rateLimiter;
    private final Duration requestTimeout;

    public WeatherProxy() {
        this(
            new ApiKeyAuthStrategy(DEFAULT_API_KEY, API_KEY_PARAM),
            new ProxyLogger(Level.INFO, Level.INFO, Level.WARNING, false),
            new RateLimiter(DEFAULT_MAX_REQUESTS, DEFAULT_RATE_LIMIT_PERIOD),
            DEFAULT_REQUEST_TIMEOUT
        );
    }

    public WeatherProxy(
            AuthorizationStrategy authStrategy,
            ProxyLogger logger,
            RateLimiter rateLimiter,
            Duration requestTimeout) {
        this.authStrategy = authStrategy;
        this.logger = logger;
        this.rateLimiter = rateLimiter;
        this.requestTimeout = requestTimeout;
        loadCityList();
    }

    private void loadCityList() {
        try (Stream<ObjectNode> stream = LargeDataProcessor.streamJsonArrayFile(
                Path.of("src/main/resources/city.list.json"))) {

            stream.map(node -> objectMapper.convertValue(node, City.class))
                    .forEach(city -> cityIdMap.put(city.getName().toLowerCase(), city.getId()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load city list", e);
        }
    }


    public WeatherData fetchWeather(String cityNameInput) throws Exception {
        Integer cityId = cityIdMap.get(cityNameInput.toLowerCase());
        if (cityId == null) {
            logger.logError("City not found: " + cityNameInput, null);
            throw new IllegalArgumentException("City not found: " + cityNameInput);
        }

        rateLimiter.acquire();

        String url = String.format("%s?id=%d&units=metric", API_URL, cityId);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                .GET();

        if (!authStrategy.isValid()) {
            authStrategy.updateCredentials();
        }
        requestBuilder = authStrategy.authorize(requestBuilder);

        HttpRequest request = requestBuilder.build();

        logger.logRequest(request);

        long startTime = System.currentTimeMillis();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            logger.logResponse(response, startTime);

            String responseBody = response.body();
            return parseWeatherData(objectMapper.readTree(responseBody));
        } catch (Exception e) {
            logger.logError("Failed to fetch weather for " + cityNameInput, e);
            throw e;
        }
    }
    public CompletableFuture<WeatherData> fetchWeatherAsync(String cityNameInput) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchWeather(cityNameInput);
            } catch (Exception e) {
                logger.logError("Failed to fetch weather asynchronously for " + cityNameInput, e);
                throw new RuntimeException("Failed to fetch weather for " + cityNameInput, e);
            }
        });
    }

    public void shutdown() {
        rateLimiter.shutdown();
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

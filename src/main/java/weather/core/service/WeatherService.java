package weather.core.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import weather.core.cache.WeatherCache;
import weather.core.model.WeatherData;
import weather.core.queue.CityPriorityQueue;
import weather.proxy.api.WeatherProxy;

public class WeatherService {
    private final WeatherProxy proxy = new WeatherProxy();
    private final WeatherCache cache;
    private final CityPriorityQueue cityQueue;
    private final ScheduledExecutorService scheduler;
    private static final Duration DEFAULT_CACHE_EXPIRATION = Duration.ofDays(1);
    private static final long DEFAULT_UPDATE_INTERVAL_HOURS = 12;
    private static final int DEFAULT_TOP_CITIES_COUNT = 5;
    public WeatherService() {
        this(DEFAULT_CACHE_EXPIRATION, DEFAULT_UPDATE_INTERVAL_HOURS);
    }
    public WeatherService(Duration cacheExpiration, long updateIntervalHours) {
        this.cache = new WeatherCache(cacheExpiration);
        this.cityQueue = new CityPriorityQueue();
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
            this::updateWeatherForTopCities,
            1,
            updateIntervalHours * 60 * 60,
            TimeUnit.SECONDS
        );
    }
    public CompletableFuture<WeatherData> getWeatherAsync(String city) {
        if (!cityQueue.contains(city)) {
            cityQueue.addCity(city);
        }
        return cache.computeWeatherDataAsync(city, this::fetchWeatherAsyncForCity)
            .thenApply(data -> {
                cityQueue.updatePriority(city);
                return data;
            });
    }
    private CompletableFuture<WeatherData> fetchWeatherAsyncForCity(String city) {
        return proxy.fetchWeatherAsync(city);
    }
    public CompletableFuture<List<WeatherData>> getGroupWeatherAsync(List<String> cities) {
        List<CompletableFuture<WeatherData>> futures = cities.stream()
            .map(this::getWeatherAsync)
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }
    public void updateWeatherForTopCities() {
        updateWeatherForTopCities(DEFAULT_TOP_CITIES_COUNT);
    }
    public void updateWeatherForTopCities(int n) {
        List<String> topCities = cityQueue.getTopNCities(n);
        if (topCities.isEmpty()) {
            return;
        }

        for (String city : topCities) {
            if (city == null) {
                continue;
            }

            if (!cache.hasFreshData(city)) {
                proxy.fetchWeatherAsync(city)
                    .thenAccept(data -> {
                        cache.putWeatherData(city, data);
                        cityQueue.addCity(city);
                    })
                    .exceptionally(ex -> {
                        cityQueue.addCity(city, Instant.EPOCH);
                        System.err.println("Error updating weather for " + city + ": " + ex.getMessage());
                        return null;
                    });
            } else {
                cityQueue.addCity(city);
            }
        }
    }

    public void clearCache() {
        cache.clearCache();
    }

    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Scheduler did not terminate");
                }
            }

            proxy.shutdown();
        } catch (InterruptedException ie) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

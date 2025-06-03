package weather.core.cache;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import weather.core.model.WeatherData;

public class WeatherCache {
    private final Memoizer<String, WeatherData> cache;

    public WeatherCache(Duration expiration) {
        this.cache = new Memoizer<>(expiration);
    }

    public boolean hasFreshData(String city) {
        if (city == null) {
            return false;
        }

        return cache.containsKey(city.toLowerCase());
    }

    public void putWeatherData(String city, WeatherData data) {
        if (city == null || data == null) {
            return;
        }

        cache.put(city.toLowerCase(), data);
    }

    public CompletableFuture<WeatherData> computeWeatherDataAsync(String city, 
            Function<String, CompletableFuture<WeatherData>> asyncFetchFunction) {
        if (city == null) {
            return CompletableFuture.completedFuture(null);
        }

        String lowerCaseCity = city.toLowerCase();

        if (cache.containsKey(lowerCaseCity)) {
            return CompletableFuture.completedFuture(cache.get(lowerCaseCity));
        }

        return asyncFetchFunction.apply(lowerCaseCity)
                .thenApply(weatherData -> {
                    if (weatherData != null) {
                        cache.put(lowerCaseCity, weatherData);
                    }
                    return weatherData;
                });
    }

    public void clearCache() {
        cache.clearCache();
    }
}

package weather.core.model;

public record WeatherData(String city, double temperature, String description,
                          double humidity, double windSpeed, double pressure,
                          double feelsLike, String country) {
}

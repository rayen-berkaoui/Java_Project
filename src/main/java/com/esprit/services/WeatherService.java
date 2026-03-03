package com.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to fetch real-time weather data using Open-Meteo API (free, no API key).
 * Uses latitude/longitude from the Adresse table for accurate weather.
 * Caches results for 10 minutes to avoid excessive API calls.
 */
public class WeatherService {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final long CACHE_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    private final HttpClient httpClient;
    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    public WeatherService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Weather data container.
     */
    public static class WeatherData {
        private final double temperature;       // °C
        private final double apparentTemp;      // °C (feels like)
        private final int humidity;             // %
        private final double windSpeed;         // km/h
        private final int weatherCode;          // WMO weather code
        private final String cityName;
        private final boolean isDay;

        public WeatherData(double temperature, double apparentTemp, int humidity,
                           double windSpeed, int weatherCode, String cityName, boolean isDay) {
            this.temperature = temperature;
            this.apparentTemp = apparentTemp;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.weatherCode = weatherCode;
            this.cityName = cityName;
            this.isDay = isDay;
        }

        public double getTemperature() { return temperature; }
        public double getApparentTemp() { return apparentTemp; }
        public int getHumidity() { return humidity; }
        public double getWindSpeed() { return windSpeed; }
        public int getWeatherCode() { return weatherCode; }
        public String getCityName() { return cityName; }
        public boolean isDay() { return isDay; }

        /** Get weather description in French based on WMO code. */
        public String getDescription() {
            return switch (weatherCode) {
                case 0 -> "Ciel dégagé";
                case 1 -> "Principalement dégagé";
                case 2 -> "Partiellement nuageux";
                case 3 -> "Couvert";
                case 45, 48 -> "Brouillard";
                case 51 -> "Bruine légère";
                case 53 -> "Bruine modérée";
                case 55 -> "Bruine dense";
                case 56, 57 -> "Bruine verglaçante";
                case 61 -> "Pluie légère";
                case 63 -> "Pluie modérée";
                case 65 -> "Pluie forte";
                case 66, 67 -> "Pluie verglaçante";
                case 71 -> "Neige légère";
                case 73 -> "Neige modérée";
                case 75 -> "Neige forte";
                case 77 -> "Grains de neige";
                case 80 -> "Averses légères";
                case 81 -> "Averses modérées";
                case 82 -> "Averses violentes";
                case 85, 86 -> "Averses de neige";
                case 95 -> "Orage";
                case 96, 99 -> "Orage avec grêle";
                default -> "Inconnu";
            };
        }

        /** Get weather emoji based on WMO weather code. */
        public String getWeatherEmoji() {
            if (weatherCode == 0) return isDay ? "☀️" : "🌙";
            if (weatherCode <= 2) return isDay ? "⛅" : "☁️";
            if (weatherCode == 3) return "☁️";
            if (weatherCode <= 48) return "🌫️";
            if (weatherCode <= 57) return "🌧️";
            if (weatherCode <= 65) return "🌧️";
            if (weatherCode <= 67) return "🌧️";
            if (weatherCode <= 77) return "❄️";
            if (weatherCode <= 82) return "🌦️";
            if (weatherCode <= 86) return "🌨️";
            if (weatherCode <= 99) return "⛈️";
            return "🌡️";
        }
    }

    private static class CachedWeather {
        final WeatherData data;
        final long timestamp;

        CachedWeather(WeatherData data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    /**
     * Fetch weather using latitude and longitude from the Adresse.
     * @param lat latitude
     * @param lon longitude
     * @param cityName display name for the city
     * @return WeatherData or null on failure
     */
    public WeatherData getWeather(double lat, double lon, String cityName) {
        String key = String.format(Locale.US, "%.2f_%.2f", lat, lon);

        // Check cache
        CachedWeather cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.data;
        }

        try {
            String url = String.format(Locale.US,
                "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,is_day&timezone=auto",
                BASE_URL, lat, lon
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "JavaFX-Tourism-App")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                WeatherData data = parseOpenMeteoJson(response.body(), cityName != null ? cityName : "");
                if (data != null) {
                    cache.put(key, new CachedWeather(data));
                    System.out.println("✅ Weather fetched for " + cityName + ": "
                        + String.format("%.0f°C", data.getTemperature()) + " — " + data.getDescription());
                }
                return data;
            } else {
                System.err.println("⚠️ Weather API returned status " + response.statusCode() + " for: " + cityName);
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ Weather API error for " + cityName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse the Open-Meteo JSON response.
     */
    private WeatherData parseOpenMeteoJson(String json, String cityName) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject current = root.getAsJsonObject("current");

            double temp = current.get("temperature_2m").getAsDouble();
            double apparentTemp = current.get("apparent_temperature").getAsDouble();
            int humidity = current.get("relative_humidity_2m").getAsInt();
            double windSpeed = current.get("wind_speed_10m").getAsDouble();
            int weatherCode = current.get("weather_code").getAsInt();
            boolean isDay = current.get("is_day").getAsInt() == 1;

            return new WeatherData(temp, apparentTemp, humidity, windSpeed, weatherCode, cityName, isDay);
        } catch (Exception e) {
            System.err.println("❌ Error parsing weather JSON: " + e.getMessage());
            return null;
        }
    }
}

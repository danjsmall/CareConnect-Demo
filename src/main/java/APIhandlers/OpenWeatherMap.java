package APIhandlers;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenWeatherMap {
    private static final String APIkey = "2e32e4d63014f13d1aa905a9682aaf77";
    private Float temperature; // °C
    private String conditionTitle; // weather/main
    private String conditionIcon; // weather/icon
    private Integer windSpeed;
    private Integer humidity;

    // Pull current weather conditions from API
    public Map<String, Object> getWeather(double lat, double lon) throws Exception {
        String WeatherURL = "https://api.openweathermap.org/data/2.5/weather" + "?lat=" + lat + "&lon=" + lon + "&appid=" + APIkey + "&units=metric";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(WeatherURL)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Weather API error: " + response.statusCode());
        }
        String body = response.body();
        JSONObject json = new JSONObject(body);
        temperature = json.getJSONObject("main").getFloat("temp");
        JSONArray weatherArray = json.getJSONArray("weather");
        if (weatherArray.length() > 0) {
            JSONObject weather = weatherArray.getJSONObject(0);
            conditionTitle = weather.getString("main");
            conditionIcon = weather.getString("icon");
        }
        JSONObject wind = json.optJSONObject("wind");
        if (wind != null) {
            windSpeed = (int) Math.round(wind.getFloat("speed") * 2.236936); // Convert m/s to mph
        }
        humidity = json.getJSONObject("main").getInt("humidity");
        Map<String, Object> result = new HashMap<>();
        result.put("temperature", temperature);
        result.put("conditionTitle", conditionTitle);
        result.put("conditionIcon", conditionIcon);
        result.put("windSpeed", windSpeed);
        result.put("humidity", humidity);
        return result;
    }

    // Get coordinates from a natural language input
    // Seems to only work with city names
    public static Object[] getCoords(String address) throws Exception {
        String geocoderURL = "https://api.openweathermap.org/geo/1.0/direct?q=" + URLEncoder.encode(address, "UTF-8") + "&limit=1&appid=" + APIkey;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(geocoderURL)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Geocoding API error: " + response.statusCode());
        }
        String body = response.body();
        JSONArray array = new JSONArray(body);
        if (array.length() == 0) {
            throw new Exception("Address not found");
        }
        JSONObject obj = array.getJSONObject(0);
        double lat = obj.getDouble("lat");
        double lon = obj.getDouble("lon");
        String name = obj.getString("name");
        return new Object[]{lat, lon, name};
    }

}
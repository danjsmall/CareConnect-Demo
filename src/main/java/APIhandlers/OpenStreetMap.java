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

public class OpenStreetMap {
    private String displayName; // Address name from geocoding
    private Double latitude;
    private Double longitude;
    private String mapTileURL; // URL for map tiles

    // Get map URL and location details for a given address
    public Map<String, Object> getMapData(String address) throws Exception {
        // First, geocode the address to get coordinates
        Object[] coords = geocodeAddress(address);
        latitude = (Double) coords[0];
        longitude = (Double) coords[1];
        displayName = (String) coords[2];
        
        // Generate map tile URL for Leaflet/OpenStreetMap
        mapTileURL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
        
        Map<String, Object> result = new HashMap<>();
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("displayName", displayName);
        result.put("mapTileURL", mapTileURL);
        return result;
    }

    // Get map URL and location details for given coordinates
    public Map<String, Object> getMapDataByCoords(double lat, double lon) throws Exception {
        latitude = lat;
        longitude = lon;
        
        // Reverse geocode to get address name
        displayName = reverseGeocodeCoords(lat, lon);
        mapTileURL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
        
        Map<String, Object> result = new HashMap<>();
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("displayName", displayName);
        result.put("mapTileURL", mapTileURL);
        return result;
    }

    // Geocode an address to get coordinates using Nominatim API
    private Object[] geocodeAddress(String address) throws Exception {
        String geocoderURL = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(address, "UTF-8") + "&format=json&limit=1";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(geocoderURL))
                .header("User-Agent", "HealthAssistant/1.0")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new Exception("Nominatim API error: " + response.statusCode());
        }
        
        String body = response.body();
        JSONArray array = new JSONArray(body);
        if (array.length() == 0) {
            throw new Exception("Address not found");
        }
        
        JSONObject obj = array.getJSONObject(0);
        double lat = Double.parseDouble(obj.getString("lat"));
        double lon = Double.parseDouble(obj.getString("lon"));
        String name = obj.getString("display_name");
        
        return new Object[]{lat, lon, name};
    }

    // Reverse geocode coordinates to get address name using Nominatim API
    private String reverseGeocodeCoords(double lat, double lon) throws Exception {
        String reverseGeocoderURL = "https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lon + "&format=json";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(reverseGeocoderURL))
                .header("User-Agent", "HealthAssistant/1.0")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new Exception("Nominatim API error: " + response.statusCode());
        }
        
        String body = response.body();
        JSONObject obj = new JSONObject(body);
        String displayName = obj.optString("display_name", "Location");
        
        return displayName;
    }

    // Get HTML content for displaying the map
    public String getMapHTML(double lat, double lon) {
        return "<!DOCTYPE html><html><head>" +
                "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css'/>" +
                "<script>L_DISABLE_3D = true;</script>" +
                "<script src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js'></script>" +
                "<style>html,body{height:100%;margin:0;overflow:hidden;}#map{width:100%;height:100%}</style>" +
                "</head><body><div id='map'></div>" +
                "<script>" +
                "L.Browser.any3d = false;" +
                "var m = L.map('map', {zoomAnimation: false}).setView([" + lat + "," + lon + "], 13);" +
                "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "    attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors'" +
                "}).addTo(m);" +
                "L.marker([" + lat + "," + lon + "]).addTo(m).bindPopup('Location').openPopup();" +
                "window.addEventListener('resize', function() { m.invalidateSize(); });" +
                "setTimeout(function() { m.invalidateSize(); }, 500);" +
                "</script></body></html>";
    }

}

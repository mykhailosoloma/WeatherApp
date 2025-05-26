package weather.core.queue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class CityPriorityQueue {
    private final PriorityQueue<CityPriority> queue;
    private final Map<String, CityPriority> cityMap;

    public CityPriorityQueue() {
        this.queue = new PriorityQueue<>(Comparator.nullsFirst(Comparator.comparing(CityPriority::lastUpdated, Comparator.nullsFirst(Comparator.naturalOrder()))));
        this.cityMap = new HashMap<>();
    }

    public void addCity(String city) {
        addCity(city, Instant.now());
    }
    public void addCity(String city, Instant lastUpdated) {
        if (city == null) {
            return;
        }

        Instant timestamp = (lastUpdated != null) ? lastUpdated : Instant.now();

        if (cityMap.containsKey(city)) {
            queue.remove(cityMap.get(city));
        }

        CityPriority cityPriority = new CityPriority(city, timestamp);
        queue.add(cityPriority);
        cityMap.put(city, cityPriority);
    }
    public String getHighestPriorityCity() {
        if (queue.isEmpty()) {
            return null;
        }

        CityPriority cityPriority = queue.poll();
        if (cityPriority == null) {
            return null;
        }

        String city = cityPriority.city();
        if (city != null) {
            cityMap.remove(city);
        }
        return city;
    }

    public List<String> getTopNCities(int n) {
        List<String> result = new ArrayList<>();

        if (n <= 0) {
            return result;
        }

        for (int i = 0; i < n && !queue.isEmpty(); i++) {
            String city = getHighestPriorityCity();
            if (city != null) {
                result.add(city);
            }
        }
        return result;
    }

    public void updatePriority(String city) {
        if (city != null) {
            addCity(city, Instant.now());
        }
    }
    public int size() {
        return queue.size();
    }
    public boolean contains(String city) {
        return city != null && cityMap.containsKey(city);
    }
    private record CityPriority(String city, Instant lastUpdated) {
    }
}

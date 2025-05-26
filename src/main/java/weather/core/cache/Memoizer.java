
package weather.core.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Memoizer<K, V> {
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final Duration expiration;

    public Memoizer(Duration expiration) {
        this.expiration = expiration;
    }

    public boolean containsKey(K key) {
        CacheEntry<V> entry = cache.get(key);
        return entry != null && isValid(entry);
    }

    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null && isValid(entry)) {
            return entry.value();
        }
        return null;
    }

    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, Instant.now()));
    }

    public void remove(K key) {
        cache.remove(key);
    }

    private boolean isValid(CacheEntry<V> entry) {
        return Duration.between(entry.timestamp(), Instant.now()).compareTo(expiration) <= 0;
    }

    private record CacheEntry<V>(V value, Instant timestamp) {
    }
}

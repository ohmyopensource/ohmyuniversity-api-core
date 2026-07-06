package org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Minimal in-memory cache with per-entry time-to-live, used to avoid
 * repeatedly hitting the slow, publicly-shared Course Catalogue endpoints
 * for data that is identical across all students of the same university,
 * cohort and course.
 *
 * <p>Not distributed — each application instance has its own cache. This is
 * acceptable because the underlying data changes at most once per academic
 * year and a cold cache simply costs one extra upstream call.
 */
public class TtlCache<V> {

  private final Map<String, Entry<V>> entries = new ConcurrentHashMap<>();
  private final Duration ttl;

  public TtlCache(Duration ttl) {
    this.ttl = ttl;
  }

  /**
   * Returns the cached value for {@code key}, computing and storing it via
   * {@code supplier} if absent or expired.
   */
  public V getOrCompute(String key, Supplier<V> supplier) {
    Entry<V> existing = entries.get(key);
    if (existing != null && existing.expiresAt.isAfter(Instant.now())) {
      return existing.value;
    }
    V computed = supplier.get();
    entries.put(key, new Entry<>(computed, Instant.now().plus(ttl)));
    return computed;
  }

  private static final class Entry<V> {
    private final V value;
    private final Instant expiresAt;

    private Entry(V value, Instant expiresAt) {
      this.value = value;
      this.expiresAt = expiresAt;
    }
  }
}
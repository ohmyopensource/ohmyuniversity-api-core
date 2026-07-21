package org.ohmyopensource.ohmyuniversity.core.cineca.coursecatalogue;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Minimal in-memory cache with per-entry time-to-live, used to avoid repeatedly hitting the slow,
 * publicly-shared Course Catalogue endpoints for data that is identical across all students of the
 * same university, cohort and course.
 *
 * <p>Not distributed — each application instance has its own cache. This is
 * acceptable because the underlying data changes at most once per academic year and a cold cache
 * simply costs one extra upstream call.
 */
public class TtlCache<V> {

  private final Map<String, Entry<V>> entries = new ConcurrentHashMap<>();
  private final Duration ttl;

  // ============ Constructor ============

  /**
   * Creates a cache where every entry expires the given duration after being stored.
   *
   * @param ttl time-to-live applied to every cached entry
   */
  public TtlCache(Duration ttl) {
    this.ttl = ttl;
  }

  // ============ Class Methods ============

  /**
   * Returns the cached value for {@code key}, computing and storing it via {@code supplier} if
   * absent or expired.
   *
   * @param key      cache key
   * @param supplier computes the value on a cache miss or expired entry
   * @return the cached or freshly computed value
   */
  public V getOrCompute(String key, Supplier<V> supplier) {
    Entry<V> result = entries.compute(key, (k, existing) -> {
      if (existing != null && existing.expiresAt.isAfter(Instant.now())) {
        return existing;
      }
      return new Entry<>(supplier.get(), Instant.now().plus(ttl));
    });
    return result.value;
  }

  private record Entry<V>(V value, Instant expiresAt) {

  }
}
package org.ohmyopensource.ohmyuniversity.core.service.kafka.event;

/**
 * Payload of the {@code campus-assignment.discovered} Kafka event.
 *
 * <p>Published by {@code KafkaEventPublisher} when a Cineca sync reveals that
 * a student is associated with a campus.
 *
 * <p>Shape must match the consumer record in {@code ohmyuniversity-canteen}
 * ({@code CampusAssignmentDiscoveredEvent}) exactly, as Jackson deserializes
 * by field name without type headers.
 *
 * <p>This event is idempotent: the canteen consumer performs an upsert,
 * so publishing the same event multiple times is safe.
 *
 * @param studentId    OhMyU user ID of the student
 * @param campusId     opaque campus identifier from the core service
 * @param universityId opaque university identifier from the core service
 */
public record CampusAssignmentDiscoveredEvent(
    String studentId,
    String campusId,
    String universityId
) {
}
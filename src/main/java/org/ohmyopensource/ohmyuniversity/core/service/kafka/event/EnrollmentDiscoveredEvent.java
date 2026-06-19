package org.ohmyopensource.ohmyuniversity.core.service.kafka.event;

/**
 * Payload of the {@code enrollment.discovered} Kafka event.
 *
 * <p>Published by {@code KafkaEventPublisher} when a Cineca sync reveals that
 * a student is enrolled in a course edition tracked by OhMyUniversity!.
 *
 * <p>Shape must match the consumer record in {@code ohmyuniversity-chat}
 * ({@code EnrollmentDiscoveredEvent}) exactly, as Jackson deserializes
 * by field name without type headers.
 *
 * @param userId            OhMyU user ID (String representation of OmuUser primary key)
 * @param externalChannelId deterministic channel ID matching the corresponding
 *                          {@code course-edition.discovered} event; the chat channel
 *                          must already exist before this event is consumed
 */
public record EnrollmentDiscoveredEvent(
    String userId,
    String externalChannelId
) {
}
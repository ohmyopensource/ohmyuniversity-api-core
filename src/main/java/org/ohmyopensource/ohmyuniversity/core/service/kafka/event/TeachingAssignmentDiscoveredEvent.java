package org.ohmyopensource.ohmyuniversity.core.service.kafka.event;

/**
 * Payload of the {@code teaching-assignment.discovered} Kafka event.
 *
 * <p>Published by {@code KafkaEventPublisher} when a Cineca sync reveals that
 * a professor is the titular holder ({@code titolareFlg = true}) of a course edition.
 *
 * <p>Shape must match the consumer record in {@code ohmyuniversity-chat}
 * ({@code TeachingAssignmentDiscoveredEvent}) exactly, as Jackson deserializes
 * by field name without type headers.
 *
 * @param userId            OhMyU user ID of the professor
 * @param externalChannelId deterministic channel ID matching the corresponding
 *                          {@code course-edition.discovered} event; the chat channel
 *                          must already exist before this event is consumed
 */
public record TeachingAssignmentDiscoveredEvent(
    String userId,
    String externalChannelId
) {
}
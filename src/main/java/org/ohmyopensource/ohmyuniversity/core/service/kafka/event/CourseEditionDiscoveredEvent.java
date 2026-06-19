package org.ohmyopensource.ohmyuniversity.core.service.kafka.event;

/**
 * Payload of the {@code course-edition.discovered} Kafka event.
 *
 * <p>Published by {@code KafkaEventPublisher} when a Cineca sync reveals a course
 * edition that has no chat channel yet in OhMyUniversity!.
 *
 * <p>The {@code externalChannelId} is a deterministic identifier built from the
 * course slug, university slug, academic year, and semester, ensuring that duplicate
 * events for the same edition are safely deduplicated by the consumer.
 *
 * <p>Shape must match the consumer record in {@code ohmyuniversity-chat}
 * ({@code CourseEditionDiscoveredEvent}) exactly, as Jackson deserializes
 * by field name without type headers.
 *
 * @param externalChannelId deterministic ID — format: {course-slug}-{university-slug}-{year}-{sem}
 * @param name              human-readable channel name (e.g. "Analisi I — UNIMOL — 2026/1")
 * @param courseId          opaque course identifier from the core service
 * @param academicYear      academic year (e.g. "2026")
 * @param semester          semester number (e.g. "1" or "2")
 */
public record CourseEditionDiscoveredEvent(
    String externalChannelId,
    String name,
    String courseId,
    String academicYear,
    String semester
) {
}
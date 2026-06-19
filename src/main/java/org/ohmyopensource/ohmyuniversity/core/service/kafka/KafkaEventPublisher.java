package org.ohmyopensource.ohmyuniversity.core.service.kafka;

import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.CampusAssignmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.CourseEditionDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.EnrollmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.TeachingAssignmentDiscoveredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka event publisher for OhMyUniversity! core service.
 *
 * <p>This class is the single point of access for publishing integration events
 * to Kafka topics. It wraps {@link KafkaTemplate} and exposes one typed method
 * per event, ensuring that callers never deal with raw topic names or serialization.
 *
 * <p>All events follow the naming convention {@code {bounded-context}.{entity}.discovered},
 * reflecting that they originate from a Cineca sync (a fact discovered by comparison
 * against local state), not from a user action inside OhMyU.
 *
 * <p>Publishing order contract for chat-related events:
 * {@code course-edition.discovered} must be sent before {@code enrollment.discovered}
 * and {@code teaching-assignment.discovered} for the same {@code externalChannelId},
 * because the chat consumer silently drops member events if the channel does not
 * exist yet.
 *
 * <p>Errors during publishing are logged but not rethrown: Kafka unavailability
 * must not block the main request/sync flow. A future improvement may add a
 * dead-letter or retry mechanism.
 */
@Service
public class KafkaEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

  private static final String TOPIC_COURSE_EDITION_DISCOVERED = "course-edition.discovered";
  private static final String TOPIC_ENROLLMENT_DISCOVERED = "enrollment.discovered";
  private static final String TOPIC_TEACHING_ASSIGNMENT_DISCOVERED =
      "teaching-assignment.discovered";
  private static final String TOPIC_CAMPUS_ASSIGNMENT_DISCOVERED = "campus-assignment.discovered";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  // ============ Constructor ============

  /**
   * Constructs a {@code KafkaEventPublisher} with the provided {@link KafkaTemplate}.
   *
   * @param kafkaTemplate Spring Kafka template used for publishing events
   */
  public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  // ============ Class Methods ============

  /**
   * Publishes a {@code course-edition.discovered} event.
   *
   * <p>Must be called before {@link #publishEnrollmentDiscovered} and
   * {@link #publishTeachingAssignmentDiscovered} for the same {@code externalChannelId}.
   *
   * @param event the event payload; must not be null
   */
  public void publishCourseEditionDiscovered(CourseEditionDiscoveredEvent event) {
    send(TOPIC_COURSE_EDITION_DISCOVERED, event.externalChannelId(), event);
  }

  /**
   * Publishes an {@code enrollment.discovered} event.
   *
   * <p>The corresponding chat channel must already exist before this event
   * is consumed (i.e. {@code course-edition.discovered} must have been sent first).
   *
   * @param event the event payload; must not be null
   */
  public void publishEnrollmentDiscovered(EnrollmentDiscoveredEvent event) {
    send(TOPIC_ENROLLMENT_DISCOVERED, event.externalChannelId(), event);
  }

  /**
   * Publishes a {@code teaching-assignment.discovered} event.
   *
   * <p>The corresponding chat channel must already exist before this event
   * is consumed (i.e. {@code course-edition.discovered} must have been sent first).
   *
   * @param event the event payload; must not be null
   */
  public void publishTeachingAssignmentDiscovered(TeachingAssignmentDiscoveredEvent event) {
    send(TOPIC_TEACHING_ASSIGNMENT_DISCOVERED, event.externalChannelId(), event);
  }

  /**
   * Publishes a {@code campus-assignment.discovered} event.
   *
   * <p>This event is idempotent: the canteen consumer performs an upsert,
   * so publishing the same event multiple times is safe.
   *
   * @param event the event payload; must not be null
   */
  public void publishCampusAssignmentDiscovered(CampusAssignmentDiscoveredEvent event) {
    send(TOPIC_CAMPUS_ASSIGNMENT_DISCOVERED, event.studentId(), event);
  }

  /**
   * Sends an event to the specified Kafka topic using the given key.
   *
   * <p>The key is used for partition routing: events with the same key
   * are guaranteed to land on the same partition, preserving order within
   * a logical entity (e.g. all events for the same channel on the same partition).
   *
   * <p>Failures are logged but not rethrown to avoid blocking the calling thread.
   *
   * @param topic the Kafka topic name
   * @param key   the partition routing key
   * @param event the event payload object
   */
  private void send(String topic, String key, Object event) {
    kafkaTemplate.send(topic, key, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("KafkaEventPublisher: failed to publish to topic={} key={}: {}",
                topic, key, ex.getMessage());
          } else {
            log.debug("KafkaEventPublisher: published to topic={} key={} partition={} offset={}",
                topic, key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }
}
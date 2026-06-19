package org.ohmyopensource.ohmyuniversity.core.service.kafka;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.CampusAssignmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.CourseEditionDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.EnrollmentDiscoveredEvent;
import org.ohmyopensource.ohmyuniversity.core.service.kafka.event.TeachingAssignmentDiscoveredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * Unit tests for {@link KafkaEventPublisher}.
 *
 * <p>{@link KafkaTemplate} is mocked via Mockito — no running Kafka instance is required.
 * Tests verify that each {@code publish*} method sends to the correct topic
 * with the correct partition key and payload.
 */
@SuppressWarnings("unchecked")
class KafkaEventPublisherTest {

  private KafkaTemplate<String, Object> kafkaTemplate;
  private KafkaEventPublisher publisher;

  /**
   * Initialises fresh mocks and a new {@link KafkaEventPublisher} instance
   * before each test to guarantee isolation.
   */
  @BeforeEach
  void setUp() {
    kafkaTemplate = mock(KafkaTemplate.class);
    publisher = new KafkaEventPublisher(kafkaTemplate);
  }

  /**
   * Builds a completed {@link CompletableFuture} wrapping a mock {@link SendResult}
   * with the given topic and partition, simulating a successful Kafka send.
   *
   * @param topic     the Kafka topic name
   * @param partition the partition number to include in record metadata
   * @return completed future with a mock SendResult
   */
  private CompletableFuture<SendResult<String, Object>> successFuture(
      String topic, int partition) {
    RecordMetadata metadata = new RecordMetadata(
        new TopicPartition(topic, partition), 0L, 0, 0L, 0, 0);
    ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(topic, "key", null);
    SendResult<String, Object> sendResult = new SendResult<>(producerRecord, metadata);
    return CompletableFuture.completedFuture(sendResult);
  }

  /**
   * Verifies the behaviour of {@link KafkaEventPublisher#publishCourseEditionDiscovered}.
   */
  @Nested
  @DisplayName("publishCourseEditionDiscovered()")
  class PublishCourseEditionDiscovered {

    /**
     * Verifies that the event is sent to the {@code course-edition.discovered} topic
     * using {@code externalChannelId} as the partition key.
     */
    @Test
    @DisplayName("sends to course-edition.discovered with externalChannelId as key")
    void sendsToCorrectTopicWithCorrectKey() {
      CourseEditionDiscoveredEvent event = new CourseEditionDiscoveredEvent(
          "an01-unimol-2026-1", "Analisi I — UNIMOL — 2026/1", "12345", "2026", "1");

      when(kafkaTemplate.send(
          eq("course-edition.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event)))
          .thenReturn(successFuture("course-edition.discovered", 0));

      publisher.publishCourseEditionDiscovered(event);

      verify(kafkaTemplate).send(
          eq("course-edition.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event));
    }
  }

  /**
   * Verifies the behaviour of {@link KafkaEventPublisher#publishEnrollmentDiscovered}.
   */
  @Nested
  @DisplayName("publishEnrollmentDiscovered()")
  class PublishEnrollmentDiscovered {

    /**
     * Verifies that the event is sent to the {@code enrollment.discovered} topic
     * using {@code externalChannelId} as the partition key.
     */
    @Test
    @DisplayName("sends to enrollment.discovered with externalChannelId as key")
    void sendsToCorrectTopicWithCorrectKey() {
      EnrollmentDiscoveredEvent event = new EnrollmentDiscoveredEvent(
          "user-uuid-123", "an01-unimol-2026-1");

      when(kafkaTemplate.send(
          eq("enrollment.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event)))
          .thenReturn(successFuture("enrollment.discovered", 1));

      publisher.publishEnrollmentDiscovered(event);

      verify(kafkaTemplate).send(
          eq("enrollment.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event));
    }
  }

  /**
   * Verifies the behaviour of {@link KafkaEventPublisher#publishTeachingAssignmentDiscovered}.
   */
  @Nested
  @DisplayName("publishTeachingAssignmentDiscovered()")
  class PublishTeachingAssignmentDiscovered {

    /**
     * Verifies that the event is sent to the {@code teaching-assignment.discovered} topic
     * using {@code externalChannelId} as the partition key.
     */
    @Test
    @DisplayName("sends to teaching-assignment.discovered with externalChannelId as key")
    void sendsToCorrectTopicWithCorrectKey() {
      TeachingAssignmentDiscoveredEvent event = new TeachingAssignmentDiscoveredEvent(
          "prof-uuid-456", "an01-unimol-2026-1");

      when(kafkaTemplate.send(
          eq("teaching-assignment.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event)))
          .thenReturn(successFuture("teaching-assignment.discovered", 2));

      publisher.publishTeachingAssignmentDiscovered(event);

      verify(kafkaTemplate).send(
          eq("teaching-assignment.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event));
    }
  }

  /**
   * Verifies the behaviour of {@link KafkaEventPublisher#publishCampusAssignmentDiscovered}.
   */
  @Nested
  @DisplayName("publishCampusAssignmentDiscovered()")
  class PublishCampusAssignmentDiscovered {

    /**
     * Verifies that the event is sent to the {@code campus-assignment.discovered} topic
     * using {@code studentId} as the partition key.
     */
    @Test
    @DisplayName("sends to campus-assignment.discovered with studentId as key")
    void sendsToCorrectTopicWithCorrectKey() {
      CampusAssignmentDiscoveredEvent event = new CampusAssignmentDiscoveredEvent(
          "student-uuid-789", "UNIMOL", "UNIMOL");

      when(kafkaTemplate.send(
          eq("campus-assignment.discovered"),
          eq("student-uuid-789"),
          eq(event)))
          .thenReturn(successFuture("campus-assignment.discovered", 0));

      publisher.publishCampusAssignmentDiscovered(event);

      verify(kafkaTemplate).send(
          eq("campus-assignment.discovered"),
          eq("student-uuid-789"),
          eq(event));
    }
  }

  /**
   * Verifies that a Kafka send failure is handled gracefully without
   * propagating exceptions to the caller.
   */
  @Nested
  @DisplayName("send() — error handling")
  class SendErrorHandling {

    /**
     * Verifies that when Kafka send completes exceptionally,
     * no exception is propagated to the caller.
     */
    @Test
    @DisplayName("Kafka send fails → exception not propagated to caller")
    void kafkaSendFailsGracefully() {
      CourseEditionDiscoveredEvent event = new CourseEditionDiscoveredEvent(
          "an01-unimol-2026-1", "Analisi I", "12345", "2026", "1");

      CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
      failedFuture.completeExceptionally(new RuntimeException("Kafka broker unavailable"));

      when(kafkaTemplate.send(
          eq("course-edition.discovered"),
          eq("an01-unimol-2026-1"),
          eq(event)))
          .thenReturn(failedFuture);

      org.assertj.core.api.Assertions.assertThatNoException()
          .isThrownBy(() -> publisher.publishCourseEditionDiscovered(event));
    }
  }
}
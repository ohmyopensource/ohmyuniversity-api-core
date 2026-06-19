package org.ohmyopensource.ohmyuniversity.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic definitions for OhMyUniversity! core service.
 *
 * <p>All topics follow the naming convention: {@code {bounded-context}.{entity}.{fact-passato}}.
 * The verb {@code discovered} is used for all events originating from a Cineca sync,
 * meaning OhMyU discovered a fact by comparing Cineca data against its local state —
 * not because a user performed an action inside OhMyU.
 *
 * <p>Topic configuration:
 * - 3 partitions: allows up to 3 parallel consumers per group, sufficient for current load
 * - replication factor 1: suitable for local development and single-broker setups;
 *   increase to 3 in production with a multi-broker cluster
 */
@Configuration
public class KafkaTopicsConfig {

  /**
   * Topic published when a Cineca sync reveals a course edition
   * that has no chat channel yet.
   *
   * <p>Consumer: {@code ohmyuniversity-chat} — creates the channel via
   * {@code ChatChannelService.createIfAbsent()}.
   *
   * <p>Must be consumed before {@code enrollment.discovered} and
   * {@code teaching-assignment.discovered} for the same {@code externalChannelId},
   * otherwise those events are silently dropped by the chat consumer.
   */
  @Bean
  public NewTopic courseEditionDiscoveredTopic() {
    return TopicBuilder.name("course-edition.discovered")
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Topic published when a Cineca sync reveals that a student
   * is enrolled in a course edition.
   *
   * <p>Consumer: {@code ohmyuniversity-chat} — adds the student as {@code STUDENT}
   * member to the corresponding chat channel.
   *
   * <p>Requires the channel to already exist (i.e. {@code course-edition.discovered}
   * must have been published and consumed first for the same {@code externalChannelId}).
   */
  @Bean
  public NewTopic enrollmentDiscoveredTopic() {
    return TopicBuilder.name("enrollment.discovered")
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Topic published when a Cineca sync reveals that a professor
   * is the titular holder ({@code titolareFlg}) of a course edition.
   *
   * <p>Consumer: {@code ohmyuniversity-chat} — adds the professor as {@code TEACHER_ADMIN}
   * member to the corresponding chat channel.
   *
   * <p>Requires the channel to already exist (i.e. {@code course-edition.discovered}
   * must have been published and consumed first for the same {@code externalChannelId}).
   */
  @Bean
  public NewTopic teachingAssignmentDiscoveredTopic() {
    return TopicBuilder.name("teaching-assignment.discovered")
        .partitions(3)
        .replicas(1)
        .build();
  }

  /**
   * Topic published when a Cineca sync reveals that a student
   * is associated with a campus.
   *
   * <p>Consumer: {@code ohmyuniversity-canteen} — upserts the student-campus association
   * so the canteen service can target the correct campus menu for that student.
   *
   * <p>This event is idempotent: the canteen consumer performs an upsert,
   * so duplicate events are safe.
   */
  @Bean
  public NewTopic campusAssignmentDiscoveredTopic() {
    return TopicBuilder.name("campus-assignment.discovered")
        .partitions(3)
        .replicas(1)
        .build();
  }
}
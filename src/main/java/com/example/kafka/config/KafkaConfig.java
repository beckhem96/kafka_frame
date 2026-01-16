package com.example.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 설정 클래스
 *
 * 역할:
 * 1. Kafka Topic 자동 생성
 * 2. KafkaProperties 활성화
 * 3. Kafka 리스너 활성화
 *
 * 동작 시점:
 * - Spring Boot 애플리케이션 시작 시
 * - 모든 @Bean 메서드 실행
 * - Bean은 Spring Container에 등록
 *
 * 참고:
 * - Producer/Consumer 설정은 application.yml에서 자동으로 적용됨
 * - 별도로 ProducerFactory, ConsumerFactory를 만들 필요 없음
 */
@Slf4j
@Configuration  // Spring 설정 클래스임을 선언
@EnableKafka    // @KafkaListener 애노테이션 활성화
@EnableConfigurationProperties(KafkaProperties.class) // KafkaProperties를 Bean으로 등록
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
public class KafkaConfig {
    // KafkaProperties를 주입받아 설정 값 사용
    private final KafkaProperties kafkaProperties;

    /**
     * Kafka Topic 자동 생성
     *
     * 동작:
     * 1. 애플리케이션 시작 시 이 메서드 실행
     * 2. KafkaProperties에서 토픽 설정 읽기
     * 3. 각 토픽을 NewTopic 객체로 생성
     * 4. Kafka Admin Client가 자동으로 토픽 생성 시도
     * 5. 토픽이 이미 존재하면 생성 스킵
     *
     * 주의:
     * - 토픽 삭제는 자동으로 안됨 (수동 삭제 필요)
     * - 파티션 개수 변경은 증가만 가능 (감소 불가)
     * - 복제본 개수는 변경 불가
     *
     * 반환:
     * - NewTopic[] 배열
     * - Spring이 이 배열의 각 NewTopic을 Kafka에 생성
     */
    @Bean
    public NewTopic[] kafkaTopics() {
        log.info("========================================");
        log.info("🔧 Creating Kafka topics...");
        log.info("========================================");

        // KafkaProperties에서 topics Map을 가져와서 순회
        // entrySet(): Map의 Key-Value 쌍을 Set으로 반환
        return kafkaProperties.getTopics().entrySet().stream()
                .map(entry -> {
                    // Key: 토픽 식별자 (예: "app-logs")
                    String key = entry.getKey();

                    // Value: TopicConfig 객체
                    KafkaProperties.TopicConfig config = entry.getValue();

                    // TopicBuilder로 토픽 생성 (Fluent API 패턴)
                    NewTopic topic = TopicBuilder
                            .name(config.getName())                    // 토픽 이름
                            .partitions(config.getPartitions())        // 파티션 개수
                            .replicas(config.getReplicationFactor())   // 복제본 개수
                            .build();

                    // 추가 설정이 있으면 적용
                    if (config.getConfigs() != null && !config.getConfigs().isEmpty()) {
                        topic.configs(config.getConfigs());
                        log.info("📝 Topic: {} (partitions: {}, replication: {}, configs: {})",
                                config.getName(),
                                config.getPartitions(),
                                config.getReplicationFactor(),
                                config.getConfigs());
                    } else {
                        log.info("📝 Topic: {} (partitions: {}, replication: {})",
                                config.getName(),
                                config.getPartitions(),
                                config.getReplicationFactor());
                    }

                    return topic;
                })
                .toArray(NewTopic[]::new);  // Stream을 배열로 변환
    }

    /**
     * ProducerFactory와 ConsumerFactory는?
     *
     * Spring Boot Auto-Configuration이 자동으로 생성해줍니다!
     *
     * 자동 생성되는 Bean:
     * - ProducerFactory<String, String>
     * - ConsumerFactory<String, String>
     * - KafkaTemplate<String, String>
     * - ConcurrentKafkaListenerContainerFactory
     *
     * application.yml의 spring.kafka.* 설정이 자동 적용됨
     *
     * 커스터마이징이 필요한 경우에만 수동으로 Bean 생성:
     *
     * @Bean
     * public ProducerFactory<String, String> producerFactory() {
     *     Map<String, Object> props = new HashMap<>();
     *     props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
     *               kafkaProperties.getBootstrapServers());
     *     // 추가 커스텀 설정...
     *     return new DefaultKafkaProducerFactory<>(props);
     * }
     */
}

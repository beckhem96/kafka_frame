package com.example.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 설정 클래스
 * 역할:
 * 1. Kafka Topic 자동 생성
 * 2. Producer 설정 및 Bean 등록
 * 3. Consumer 설정 및 Bean 등록
 * 4. KafkaTemplate 제공
 * 동작 시점:
 * - 애플리케이션 시작 시 모든 @Bean 메서드가 실행됨
 * - 각 Bean은 Spring Container에 등록되어 필요한 곳에 주입된
 */
@Slf4j
@EnableKafka // @KafkaListener 어노테이션 활성화
@Configuration // Spring Bean으로 등록, 설정 클래스임을 선언
@EnableConfigurationProperties(KafkaProperties.class)
@RequiredArgsConstructor
public class KafkaConfig {

    // KafkaProperties를 주입받아 설정 값 사용
    private final KafkaProperties kafkaProperties;

    /**
     * ===== 1단계: Kafka Topic 자동 생성 =====
     *
     * 동작:
     * 1. 애플리케이션 시작 시 이 Bean이 생성됨
     * 2. yml에 정의된 topics를 순회하며 NewTopic 객체 생성
     * 3. Kafka Admin Client가 자동으로 토픽 생성 시도
     * 4. 토픽이 이미 존재하면 생성 스킵
     *
     * 주의:
     * - 토픽 삭제는 자동으로 안된 (수동 삭제 필요)
     * - 파티션 개수 변경은 증가만 가능 (감소 불가)
     */
    @Bean
    public NewTopic[] kafkaTopics() {
        log.info("Creating Kafka topics...");

        // Map의 entrySet()으로 각 토픽 설정을 순회
        return kafkaProperties.getTopics().entrySet().stream()
                .map(entry -> {

                    // entry.getValue()로 TopicConfig 객체 가져오기
                    KafkaProperties.TopicConfig config = entry.getValue();

                    // TopicBuilder로 토픽 생성 (Fluent API 패턴)
                    TopicBuilder builder = TopicBuilder
                            .name(config.getName()) // 토픽 이름
                            .partitions(config.getPartitions()) // 파티션 개수
                            .replicas(config.getReplicationFactor()); // 복제본 개수

                    // 추가 설정이 있으면 적용 (retention.ms, cleanup.policy 등)
                    if (config.getConfigs() != null) {
                        config.getConfigs().forEach(builder::config);
                    }

                    log.info("Topic created: {} (partitions: {}, replication: {})",
                            config.getName(),
                            config.getPartitions(),
                            config.getReplicationFactor());
                    return builder.build();
                })
                .toArray(NewTopic[]::new); // 배열로 반환
    }

    /**
     * ===== 2단계: Producer Factory 생성 =====
     *
     * 역할:
     * - Producer 인스턴스를 생성하는 팩토리
     * - 연결 설정, 직렬화 방식 등을 정의
     *
     * 동작:
     * 1. Kafka 연결에 필요한 설정을 Map에 담음
     * 2. DefaultKafkaProducerFactory에 설정 전달
     * 3. KafkaTemplate이 이 팩토리를 사용해 Producer 생성
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        // Kafka Producer 설정을 담는 Map
        Map<String, Object> props = new HashMap<>();

        // ===== 필수 설정 =====

        // 1. Kafka 브로커 주소
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        // 2. Key 직렬화 방식 (String -> byte[])
        // Key는 메시지를 어는 파티션으로 보낼지 결정하는 사용
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringBuilder.class);

        // 3. Value 직렬화 방식 (String -> byte[])
        // 실제 메시지 내용
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringBuilder.class);

        // ===== Properties에서 가져온 설정 =====

        KafkaProperties.Producer producer = kafkaProperties.getProducer();

        // 4. Acks (메시지 전송 확인 레벨)
        props.put(ProducerConfig.ACKS_CONFIG, producer.getAcks());

        // 5. Retries (재시도 횟수)
        props.put(ProducerConfig.RETRIES_CONFIG, producer.getRetries());

        // 6. Batch Size (배치 크기)
        // 이 크기만큼 메시지가 모이면 한번에 전송
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.getBatchSize());

        // 7. Linger MS (배치 대기 시간)
        // batch-size에 도달하지 않아도 이 시간이 지나면 전송
        props.put(ProducerConfig.LINGER_MS_CONFIG, producer.getLingersMs());

        // 8. Compression Type (압축 알고리즘)
        // 네트워크 대역폭 절약
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producer.getCompressionType());

        // ===== 추가 권장 설정 =====

        // 9. Idempotence (멱등성 활성화)
        // 중복 메시지 전송 방지 (네트워크 재전송 시)
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        log.info("Producer factory configured with acks: {}, retries: {}", producer.getAcks(), producer.getRetries());

        // DefaultKafkaProducerFactory 생성 및 반환
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * ===== 3단계: KafkaTemplate 생성 =====
     * 역할:
     * - Kafka로 메시지를 보내는 고수준 API
     * - ProducerFactory를 사용해 Producer 인스턴스 관리
     *
     * 사용:
     * - Service 클래스에서 주입받아 사용
     * - kafkaTemplate.send(topcie, key, value)로 메시지 전송
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        // ProducerFactory를 사용해 KafkaTemplate 생성
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * ===== 4단계: Consumer Factory 생성 =====
     *
     * 역할:
     * - Consumer 인스턴스를 생성하는 팩토리
     * - 연결 설정, 역직렬화 방식 등을 정의
     *
     * 동작:
     * 1. Kafka 연결에 필요한 설정을 Map에 담음
     * 2. DefaultKafkaConsumerFactory레 설정 전달
     * 3. @KafkaListener가 이 팩토리를 사용해 Consumer 생성
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        // Kafka Consumer 설정을 담는 Map
        Map<String, Object> props = new HashMap<>();

        // ===== 필수 설정 =====

        // 1. Kafka 브로커 주소
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());

        // 2. Key 역직렬화 방식 (byte[] -> String)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 3. Value 역직렬화 방식 (byte[] -> String)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // ===== Properties에서 가져온 설정 =====

        KafkaProperties.Consumer consumer = kafkaProperties.getConsumer();

        // 4. Group ID (Consumer 그룹 식별자)
        // 같은 그룹의 Consumer들은 메시지를 분산해서 처리
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.getGroupId());

        // 5. Auto Offset Reset (오프셋 초기화 전략)
        // earliest: 처음부터, latest: 최신부터
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.getAutoOffsetReset());

        // 6. Max Poll Records (한 번에 가져올 최대 레코드 수)
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumer.getMaxPollRecords());

        // 7. Enable Auto Commit (자동 커밋 여부)
        // false로 설정하면 수동으로 acknowledgement.acknowledge() 호출 필요
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumer.getEnableAutoCommit());

        // ===== 추가 권장 설정 =====

        // 8. Error Handling Deserializer (역직렬화 에러 처리)
        // 역직렬화 실패 시에도 메시지 소비 계속 진행
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);

        log.info("Consumer factory configured with group: {}, auto-offset-reset: {}", consumer.getGroupId(), consumer.getAutoOffsetReset());

        // DefaultKafkaConsumerFactory 생성 및 반환
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * ===== 5단계: Kafka Listener Container Factory 생성 =====
     *
     * 역할:
     * - @KafkaListener가 사용하는 컨테이너 설정
     * - Consumer의 동시성, 에러 처리, 커밋 방식 등 설정
     *
     * 동작:
     * 1. @KafkaListener 메서드가 실행될 때 이 팩토리를 사용
     * 2. 설정된 개수만큼 Consumer Thread 생성
     * 3. 각 Thread가 파티션을 할당받아 메시지 처리
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        // ConcurrentKafkaListenerContainerFactory 생성
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();

        // 1. ConsumerFactory 설정
        // 이 팩토리로 Consumer 인스턴스 생성
        factory.setConsumerFactory(consumerFactory());

        // 2. Ack Mode 설정 (커밋 방식)
        // - MANUAL: acknowledgement.acknowledge() 호출 시 커밋
        // - MANUAL_IMMEDIATE: acknowledge() 호출 즉시 커밋
        // - BATCH: 배치 단위로 자동 커밋
        // - RECORD: 레코드 단위로 자동 커밋
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // 3. Concurrency 설정 (동시 실행 Consumer 개수)
        // 파티션 개수만큼 설정하는 것이 이상적
        // 예: 파티션 3개면 concurrency 3
        factory.setConcurrency(3);

        // 4. Error Handler 설정
        // Consumer에서 예외 발생 시 처리 방법
        // DefaultErrorHandler: 재시도 후 로깅
        factory.setCommonErrorHandler(
                new DefaultErrorHandler()
        );

        log.info("Kafka listener container factory configured with concurrency: 3");

        return factory;
    }
}

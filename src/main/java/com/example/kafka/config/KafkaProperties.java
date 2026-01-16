package com.example.kafka.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * Kafka 설정을 Java 객체로 매핑
 *
 * application.yml의 app.kafka.* 설정을 이 클래스로 바인딩
 *
 * 동작:
 * 1. Spring Boot 시작 시 yml 파일 읽기
 * 2. @ConfigurationProperties로 자동 바인딩
 * 3. @Validated로 유효성 검증
 * 4. Bean으로 등록되어 다른 클래스에서 주입받아 사용
 */
@ConfigurationProperties(prefix = "app.kafka")  // app.kafka로 시작하는 설정을 이 클래스에 매핑
@Validated  // javax.validation 검증 활성화
@Getter
@Setter
public class KafkaProperties {

    /**
     * Kafka 브로커 주소
     *
     * 예시:
     * - 단일 브로커: "localhost:9092"
     * - 다중 브로커: "broker1:9092,broker2:9092,broker3:9092"
     *
     * 주의:
     * - 호스트에서 실행 시: localhost:29092
     * - 컨테이너에서 실행 시: kafka:9092
     */
    @NotBlank(message = "Bootstrap servers는 필수입니다")
    private String bootstrapServers;

    /**
     * 토픽 설정 Map
     *
     * Key: 토픽 식별자 (예: "app-logs", "error-logs")
     * Value: TopicConfig 객체 (파티션, 복제본 등)
     *
     * 사용 예:
     * String topicName = kafkaProperties.getTopics().get("app-logs").getName();
     */
    private Map<String, TopicConfig> topics;

    /**
     * 개별 토픽 설정
     *
     * 내부 클래스로 정의하여 yml 구조와 동일하게 매핑
     */
    @Getter
    @Setter
    public static class TopicConfig {

        /**
         * 실제 Kafka에 생성될 토픽 이름
         *
         * 예: "app-logs", "error-logs", "access-logs"
         */
        @NotBlank(message = "토픽 이름은 필수입니다")
        private String name;

        /**
         * 파티션 개수
         *
         * 파티션이란?
         * - 토픽을 나누는 물리적 단위
         * - 파티션 개수만큼 병렬 처리 가능
         * - Consumer 개수는 파티션 개수 이하여야 효율적
         *
         * 권장:
         * - 일반 로그: 2-3개
         * - 대용량 로그: 5-10개
         * - 실시간 이벤트: 10개 이상
         */
        @Min(value = 1, message = "파티션은 최소 1개 이상이어야 합니다")
        private Integer partitions = 1;

        /**
         * 복제본 개수 (Replication Factor)
         *
         * 복제본이란?
         * - 데이터 백업 개수
         * - 브로커 장애 시 데이터 손실 방지
         * - 브로커 개수보다 클 수 없음
         *
         * 권장:
         * - 개발 환경: 1
         * - 운영 환경: 2-3
         * - 미션 크리티컬: 3 이상
         */
        @Min(value = 1, message = "복제본은 최소 1개 이상이어야 합니다")
        private Short replicationFactor = 1;

        /**
         * 추가 토픽 설정
         *
         * Kafka 토픽의 세부 설정
         * 예:
         * - retention.ms: 메시지 보관 기간 (밀리초)
         * - cleanup.policy: 정리 정책 (delete, compact)
         * - compression.type: 압축 방식
         */
        private Map<String, String> configs;
    }
}

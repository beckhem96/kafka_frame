package com.example.kafka.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Kafka 설정을 Java 객체로 매핑하는 클래스
 * 동작 원리:
 * 1. @ConfigurationProperties로 "app.kafka" prefix의 설정을 읽음
 * 2. Spring Boot가 자동으로 yml의 값을 필드에 주입
 * 3. @Validated로 유효성 검증 수행
 * 4. Bean으로 등록되어 다른 클래스에서 주입받아 사용
 */
@ConfigurationProperties(prefix = "app.kafka")
@Getter
@Setter
public class KafkaProperties {

    /**
     * Kafka 브로커 서버 주소
     * 예: "localhost:9092" 또는 "server1:9092,server2:9092,server3:9092"
     */
    @NotBlank(message = "Bootstrap servers는 필수입니다")
    private String bootstrapServers;

    /**
     * 토픽 설정들을 담는 Map
     * Key: 토픽 식별자 (예: "chat", "notification")
     * Value: TopicConfig 객체 (파티션, 복제본 등 상세 설정)
     */
    private Map<String, TopicConfig> topics;

    /**
     * Consumer 관련 설정
     */
    private Consumer consumer;

    /**
     * Producer 관련 설정
     */
    private Producer producer;

    /**
     * 개별 토픽 설정을 담는 내부 클래스
     */
    @Getter
    @Setter
    public static class TopicConfig {
        /**
         * Kafka에 실제로 생성된 토픽 이름
         */
        @NotBlank
        private String name;

        /**
         * 파티션 개수
         * - 파티션은 토픽을 나누는 물리적 단위
         * - 파티션 개수만큼 병렬 처리 가능
         * - Consumer 개수는 파티션 개수 이하여야 효율적
         */
        @Min(1)
        private Integer partitions = 1;

        /**
         * 복제본 개수 (Replication Factor)
         * - 데이터 백업 개수
         * - 브로커 개수보다 클 수 없음
         * - 운영 환경에서는 최소 2 ~ 3 권장
         */
        @Min(1)
        private Short replicationFactor = 1;

        /**
         * 추가 토픽 설정 (retention.ms, cleanup.policy 등)
         */
        private Map<String, String> configs;
    }

    /**
     * Consumer 설정 내부 클래스
     */
    @Getter
    @Setter
    public static class Consumer {
        /**
         * Consumer Group ID
         * - 같은 그룹의 Consumer들은 메시지를 분산해서 처리
         * - 각 파티션은 그룹 내 하나의 Consumer에만 할당
         */
        @NotBlank
        private String groupId;

        /**
         * 오프셋 초기화 전략
         * - earliest: 토픽의 처음부터 읽음
         * - latest: 현재 시점 이후 메시지만 읽음
         * - none: 오프셋이 없으면 예외 발생
         */
        private String autoOffsetReset = "earliest";

        /**
         * 한 번의 poll()로 가져올 최대 레코드 수
         * - 작으면: 지연 시간 감소, 처리량 감소
         * - 크면: 처리량 증가, 메모리 사용 증가
         */
        @Min(1)
        private Integer maxPollRecords = 100;

        /**
         * 자동 커밋 여부
         * - true: 일정 간격으로 자동 커밋 (간편, 중복 처리 가능)
         * - false: 수동 커밋 (정확, 중복 방지)
         */
        private Boolean enableAutoCommit = false;
    }

    /**
     * Producer 설정 내부 클래스
     */
    @Getter
    @Setter
    public static class Producer {
        /**
         * 메시지 전송 확인 레밸
         * - all" 모든 복제본이 머세지를 받았는지 확인 (가장 안전)
         * - 1: 리더 브로커만 확인 (균형)
         * - 0: 확인 안함 (가장 빠름, 데이터 손실 가능)
         */
        private String acks = "all";

        /**
         * 전송 실패 시 재시도 횟수
         * - 네트워크 일시적 장애 대응
         */
        @Min(0)
        private Integer retries = 3;

        /**
         * 배치 크기 (바이트)
         * - 이 크기만큼 메시지가 쌓이면 한번에 전송
         * - 크면: 처리량 증가, 지연 시간 증가
         */
        @Min(0)
        private Integer batchSize = 16384;

        /**
         * 배치 대기 시간 (밀리초)
         * - batch-size에 도달하지 않아도 이 시간이 지나면 전송
         * - 0: 즉시 전송
         * - 크면: 뱌치 효율 증가, 지연 시간 증가
         */
        @Min(0)
        private Long lingersMs = 10L;

        /**
         * 압축 알고리즘
         * - none: 압축 안함
         * - gzip: 높은 압축률, 느림
         * - snappy: 균형 (권장)
         * - lz4: 빠름, 낮은 압축률
         * - zstd: 최신, 높은 압축률
         */
        private String compressionType = "snappy";
    }
}

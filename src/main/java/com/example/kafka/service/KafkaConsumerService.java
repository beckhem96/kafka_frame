package com.example.kafka.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *  Kafka Consumer 서비스
 *
 * 역할:
 * - Kafka Topic에서 메시지를 수신
 * - 수신한 메시지 처리 (비즈니스 로직 실행)
 * - 처리 완료 후 오프셋 커밋
 *
 * 동작 원리:
 * 1. 애플리케이션 시작 시 @KafkaListener 메서드 등록
 * 2. Spring kafka가 백그라운드 Thread 생성
 * 3. Kafka Broker에 연결해서 토픽 구독
 * 4. 새 메시지 도착 시 자동으로 메서드 실행
 * 5. 처리 완료 후 오프셋 커밋 (진행 상황 저장)
 *
 * Offset이란
 * - 각 파티션에서 메시지의 위치 (순번)
 * - Consumer가 어디까지 읽었는지 기록
 * - 재시작 시 이 위치부터 다시 읽음
 */
@Slf4j
@Service
public class KafkaConsumerService {

    /**
     * ===== 패턴 1: 기본 Consumer (가장 간단) =====
     *
     * 동작:
     * 1. "chat-topic"의 새 메시지를 자동으로 받음
     * 2. 메시지를 String으로 역직렬화
     * 3. processMessage() 메서드 실행
     * 4. 처리 완료 시 자동 커밋 (설정에 따라)
     *
     * 특징:
     * - 가장 간단한 형태
     * - 메시지 내용만 받음 (메타데이터 없음)
     * - 자동 커밋
     *
     * 사용 케이스:
     * - 간단한 메시지 처리
     * - 메타데이터가 필요 없는 경우
     */
    @KafkaListener(topics = "chat-topic", groupId = "chat-consumer-group")
    public void consumeBasic(String message) {
        log.info("Received message: {}", message);

        // Bm
        processMessage(message);
    }

    /**
     * ===== 패턴 2: 상세 정보 포함 Consumer (권장) =====
     *
     * 동작:
     * 1. @Payload로 메시지 내용 받음
     * 2. @Header로 메타데이터 받음 (Key, Partition, Offset 등)
     * 3. 비즈니스 로직 처리
     * 4. acknowledgment.acknowledge()로 수동 커밋
     *
     * 파라미터 설명:
     * - @Payload String message: 메시지 본문
     * - @Header(KafkaHeaders.RECEIVED_KEY) String key: 메시지 Key
     * - @Header(KafkaHeaders.RECEIVED_PARTITION) int partition: 파티션 번호
     * - @Header(KafkaHeaders.OFFSET) long offset: 오프셋
     * - @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp: 전송 시간
     * - Acknowledgment acknowledgment: 수동 커밋용 객체
     *
     * 수동 커밋의 장점:
     * - 처리 성공 후에만 커밋 (중복 방지)
     * - 에러 발생 시 재처리 가능
     * - 정확한 처리 보장
     *
     * 사용 케이스:
     * - 실무 대부분의 상황
     * - 정확한 처리가 필요한 경우
     * - 메타데이터 활용이 필요한 경우
     */
    @KafkaListener(
            topics = "chat-topic",
            groupId = "chat-consumer-group",
            containerFactory = "kafkaListenerContainerFactory" // Config에서 설정한 팩토리
    )
    public void consumeWithDetails(
            @Payload String message, // 메시지 본문
            @Header(KafkaHeaders.RECEIVED_KEY) String key, // 메시지 key
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, // 파티션 번호
            @Header(KafkaHeaders.OFFSET) long offset, // 오프셋
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp, // 타임스탬프
            Acknowledgment acknowledgment) { // 수동 커밋용

        // 1. 타임스탬프롤 LocalDateTime으로 변환
        LocalDateTime receivedTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );

        // 2. 메시지 정보 로깅
        log.info("Message Details:");
        log.info("   - Key: {}", key);
        log.info("   - Message: {}", message);
        log.info("   - Partition: {}", partition);
        log.info("   - Offset: {}", offset);
        log.info("   - Timestamp: {}", receivedTime);

        try {
            // 3. 비즈니스 로직 처리
            processMessage(message);

            // 4. 처리 성공 시 수동 커밋
            // 이 시점에 오프셋이 Kafka에 저장됨
            // 재시작 시 이 다음 메시지부터 읽음
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.info("Message acknowledged: offset={}", offset);
            }
        } catch (Exception e) {
            // 5. 에러 발생 시 처리
            log.error("Failed to process message: {}", e.getMessage(), e);

            // 옵션 1: 커밋하지 않음 (재시작 시 재처리)
            // 옵션 2: DLQ(Dead Letter Queue)로 전송
            // 옵션 3: 재시도 로직 실행
            // 현재: 로그만 남기고 커밋하지 않음
        }
    }

    /**
     * ===== 패턴 3: ConsumerRecord로 받기 (전체 정보 접근) =====
     *
     * 동작:
     * - ConsumerRecord 객체로 모든 정보를 한번에 받음
     * - record.topic(), record.key(), record.value() 등으로 접근
     *
     * 장점:
     * - 모든 메타데이터에 접근 가능
     * - Header도 접근 가능 (record.headers())
     *
     * 사용 케이스:
     * - 커스텀 Header 정보가 필요한 경우
     * - 더 상세한 메타데이터가 필요한 경우
     */
    @KafkaListener(
            topics = "chat-topic",
            groupId = "chat-consumer-group-record" // 다른 그룹 ID 사용
    )
    public void consumeRecord(ConsumerRecord<String, String> record) {
        // ConsumerRecord에서 모든 정보 추출
        log.info("ConsumerRecord:");
        log.info("   - Topic: {}", record.topic());
        log.info("   - Key: {}", record.key());
        log.info("   - Value: {}", record.value());
        log.info("   - Partition: {}", record.partition());
        log.info("   - Offset: {}", record.offset());
        log.info("   - Timestamp: {}",
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(record.timestamp()),
                        ZoneId.systemDefault()
                )
        );

        // Headers 정보도 접근 가능
        record.headers().forEach(header -> {
            log.info("   - Header: {} = {}",
                    header.key(),
                    new String(header.value())
            );
        });

        processMessage(record.value());
    }

    /**
     * ===== 패턴 4: 여러 토픽 동시 리스닝 =====
     *
     * 동작:
     * - 여러 토픽을 한 Consumer에서 처리
     * - @Header로 어느 토픽에서 온 메시지인지 확인
     * - 토픽별로 다른 처리 로직 실행
     *
     * 사용 케이스:
     * - 관련된 여러 토픽을 함께 처리
     * - 공통 처리 로직이 있는 경우
     */
    @KafkaListener(
            topics = {"chat-topic", "notification-topic"},
            groupId = "multi-topic-group"
    )
    public void consumeMultiTopics(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) { // 토픽 이름 받기

        log.info("Message from topic '{}': {}", topic, message);

        // 토픽별로 다른 처리
        if ("chat-topic".equals(topic)) {
            processChatMessage(message);
        } else if ("notification-topic".equals(topic)) {
            processNotification(message);
        }
    }

    /**
     * ===== 패턴 5: 특정 파티션만 리스닝 =====
     *
     * 동작:
     * - 특정 파티션의 메시지만 처리
     * - 다른 파티션은 무시
     *
     * 사용 케이스:
     * - 파티션별로 다른 처리가 필요한 경우
     * - 특정 파티션만 모니터링하고 싶을 때
     */
    @KafkaListener(
            topicPartitions = @org.springframework.kafka.annotation.TopicPartition(
                    topic = "chat-topic",
                    partitions = {"0", "1"}  // 파티션 0, 1만
            ),
            groupId = "partition-specific-group"
    )
    public void consumeSpecificPartitions(String message) {
        log.info("Message from specific partitions (0, 1): {}", message);
        processMessage(message);
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 일반 메시지 처리
     *
     * 실제 프로젝트에서는:
     * - DB에 저장
     * - 다른 서비스 API 호출
     * - 캐시 업데이트
     * - 알림 전송
     * - 이메일 발송
     * 등의 작업 수행
     */
    private void processMessage(String message) {
        log.info("Processing message: {}", message);

        try {
            // 예: 메시지 처리 시뮬레이션
            // 실제로는 DB 저장, API 호출 등
            Thread.sleep(100);

            log.info("Message processed successfully");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing interrupted", e);
        }
    }

    /**
     * 채팅 메시지 전용 처리
     */
    private void processChatMessage(String message) {
        log.info("Processing chat message: {}", message);
        // 채팅 관련 처리
        // 예: 채팅방에 메시지 전달, WebSocket 푸시 등
    }

    /**
     * 알림 메시지 전용 처리
     */
    private void processNotification(String message) {
        log.info("Processing notification: {}", message);
        // 알림 관련 처리
        // 예: Push 알림 전송, 이메일 발송 등
    }
}

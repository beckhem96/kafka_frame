package com.example.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * Kafka Producer Service
 *
 * 역할:
 * 1. 비즈니스 이벤트를 Kafka로 전송
 * 2. 에러 처리 중앙화
 * 3. 전송 결과 모니터링
 *
 * 사용 케이스:
 * - 주문 생성 이벤트
 * - 결제 완료 이벤트
 * - 사용자 행동 추적
 * - Logback이 아닌 직접 Kafka 전송이 필요한 경우
 *
 * 주의:
 * - 일반 로그는 Logback Kafka Appender 사용 (자동)
 * - 비즈니스 이벤트만 이 Service 사용 (수동)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    // Spring Boot Auto-Configuration이 자동 생성한 KafkaTemplate
    private final KafkaTemplate<String, String> kafkaTemplate;

    // JSON 직렬화용
    private final ObjectMapper objectMapper;

    /**
     * 비동기 메시지 전송 (권장!)
     *
     * 동작:
     * 1. Kafka로 메시지 전송 (non-blocking)
     * 2. 결과를 CompletableFuture로 반환
     * 3. 전송 성공/실패를 콜백으로 처리
     *
     * 장점:
     * - 빠름 (blocking 없음)
     * - 애플리케이션 성능에 영향 최소화
     *
     * 단점:
     * - 전송 실패를 즉시 알 수 없음
     *
     * 사용 예:
     * producerService.sendMessage("events", "key123", event);
     *
     * @param topic 토픽 이름
     * @param key 파티션 키 (같은 key는 같은 파티션으로)
     * @param message 전송할 메시지
     */
    public void sendMessage(String topic, String key, String message) {
        try {
            // Kafka로 비동기 전송
            // whenComplete(): 전송 완료 시 콜백
            kafkaTemplate.send(topic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            // ✅ 전송 성공
                            logSuccess(topic, key, result);
                        } else {
                            // ❌ 전송 실패
                            logFailure(topic, key, message, ex);
                        }
                    });

        } catch (Exception e) {
            // KafkaTemplate.send() 호출 자체가 실패한 경우
            log.error("❌ Exception while sending message to topic={}, key={}, error={}",
                    topic, key, e.getMessage(), e);

            // 실패한 메시지를 Dead Letter Queue나 DB에 저장
            handleSendException(topic, key, message, e);
        }
    }

    /**
     * 동기 메시지 전송 (신중하게 사용!)
     *
     * 동작:
     * 1. Kafka로 메시지 전송
     * 2. 결과를 기다림 (blocking)
     * 3. 성공/실패를 즉시 반환
     *
     * 장점:
     * - 전송 결과를 즉시 알 수 있음
     * - 트랜잭션 처리 가능
     *
     * 단점:
     * - 느림 (blocking)
     * - 애플리케이션 성능 저하
     *
     * 사용 케이스:
     * - 중요한 금융 거래
     * - 전송 실패 시 롤백 필요
     * - 결과를 즉시 확인해야 하는 경우
     *
     * @param topic 토픽 이름
     * @param key 파티션 키
     * @param message 전송할 메시지
     * @return 전송 성공 여부
     */
    public boolean sendMessageSync(String topic, String key, String message) {
        try {
            // get(): 결과를 기다림 (blocking!)
            SendResult<String, String> result = kafkaTemplate.send(topic, key, message).get();

            // 전송 성공
            logSuccess(topic, key, result);
            return true;

        } catch (Exception e) {
            // 전송 실패
            log.error("❌ Failed to send message synchronously to topic={}, key={}, error={}",
                    topic, key, e.getMessage(), e);

            handleSendException(topic, key, message, e);
            return false;
        }
    }

    /**
     * 객체를 JSON으로 변환하여 전송
     *
     * 사용 예:
     * OrderEvent event = new OrderEvent(...);
     * producerService.sendObject("orders", orderId, event);
     *
     * @param topic 토픽 이름
     * @param key 파티션 키
     * @param object 전송할 객체 (JSON으로 변환됨)
     */
    public void sendObject(String topic, String key, Object object) {
        try {
            // 객체를 JSON 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(object);

            // Kafka로 전송
            sendMessage(topic, key, jsonMessage);

        } catch (JsonProcessingException e) {
            // JSON 직렬화 실패
            log.error("❌ Failed to serialize object to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * 전송 성공 로깅
     */
    private void logSuccess(String topic, String key, SendResult<String, String> result) {
        log.debug("✅ Message sent successfully: topic={}, key={}, partition={}, offset={}",
                topic,
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    /**
     * 전송 실패 로깅
     */
    private void logFailure(String topic, String key, String message, Throwable ex) {
        log.error("❌ Failed to send message: topic={}, key={}, error={}",
                topic, key, ex.getMessage(), ex);
    }

    /**
     * 전송 예외 처리
     *
     * 실무에서는:
     * - Dead Letter Queue에 저장
     * - DB에 실패 기록 저장
     * - 알람 전송
     * - 재시도 큐에 추가
     */
    private void handleSendException(String topic, String key, String message, Exception e) {
        // TODO: Dead Letter Queue 처리
        // TODO: 실패 메시지 DB 저장
        // TODO: 모니터링 알람

        log.warn("⚠️ Message send failed, consider implementing DLQ: topic={}, key={}",
                topic, key);
    }
}

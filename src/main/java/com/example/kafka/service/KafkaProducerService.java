package com.example.kafka.service;

import com.example.kafka.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer 서비스
 *
 * 역할:
 * - Kafka로 메시지를 전송하는 비즈니스 로직
 * - 동기/비동기 전송 지원
 * - 전송 결과 처리 (성공/실패)
 *
 * KafkaTemplate 동작:
 * 1. send() 메서드 호출
 * 2. ProducerFactory를 사용해 Producer 인스턴스 가져옴
 * 3. Serializer로 Key/Value를 byte[]로 변환
 * 4. Partitioner로 파티션 결정 (Key의 해시값 기반)
 * 5. Kafka 브로커로 전송
 * 6. CompletableFuture로 결과 반환
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    // KafkaTemplate: 메시지 전송 API
    private final KafkaTemplate<String, String> kafkaTemplate;

    // KafkaProperties: 토픽 이름 등 설정 정보
    private final KafkaProperties kafkaProperties;

    /**
     * ===== 방법 1: 동기 방식 메시지 전송 =====
     *
     * 동작:
     * 1. KafkaTemplate.send() 호출 -> CompletableFuture 반환
     * 2. get() 호출로 결과를 기다림 (블로킹)
     * 3. 전송 완료될 때까지 Thread가 대기
     * 4. 결과 반환 (성공: SendResult, 실패: Exception)
     *
     * 장점:
     * - 구현이 간단
     * - 전송 성공 여부를 즉시 확인 가능
     *
     * 단점:
     * - 성능이 낮음 (Thread가 대기)
     * - 대량 메시지 전송 시 병목
     *
     * 사용 케이스:
     * - 테스트
     * - 중요한 메시지 (반드시 확인 필요)
     * - 처리량이 적은 경우
     */
    public void sendSync(String user, String message) {
        // yml에서 chat 토픽의 이름 가져오기
        String topicName = kafkaProperties.getTopics().get("chat").getName();

        try {
            // 1. 메시지 전송 (비동기로 시작)
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, user, message);

            // 2. get() 호출로 결과를 기다림 (여기서 블로킹됨!)
            SendResult<String, String> result = future.get();

            // 3. 전송 성공 - 메타데이터 로깅
            log.info("✅ Message sent successfully:");
            log.info("   - Topic: {}", topicName);
            log.info("   - Partition: {}", result.getRecordMetadata().partition());
            log.info("   - Offset: {}", result.getRecordMetadata().offset());
            log.info("   - Timestamp: {}", result.getRecordMetadata().timestamp());
        } catch (Exception e) {
            // 4. 전송 실패 - 에러 로깅 및 예외 발생
            log.error("Failed to send message: {}", e.getMessage(), e);
            throw new RuntimeException("메시지 전송 실패", e);
        }
    }

    /**
     * ===== 방법 2: 비동기 방식 메시지 전송 (권장) =====
     *
     * 동작:
     * 1. KafkaTemplate.send() 호출 -> CompletableFuture 반환
     * 2. whenComplete()로 콜백 등록
     * 3. 메서드 즉시 반환 (논블로킹)
     * 4. 전송 완료 시 별도 Thread에서 콜백 실행
     *
     * 장점:
     * - 높은 성능 (Thread가 대기하지 않음)
     * - 대량 메시지 전송에 적합
     * - 다른 작업을 동시에 수행 가능
     *
     * 단점:
     * - 구현이 약간 복잡
     * - 에러 처리를 콜백에서 해야 함
     *
     * 사용 케이스:
     * - 대부분의 실무 상황
     * - 채팅, 로깅, 이벤트 발행 등
     */
    public void sendAsync(String user, String message) {
        // yml에서 chat 토픽의 이름 가져오기
        String topicName = kafkaProperties.getTopics().get("chat").getName();

        // 1. 비동기 메시지 전송
        // - 즉시 CompletableFuture 반환
        // - 백그라운드에서 전송 진행
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, user, message);

        // 2. 콜백 등록 (전송 완료 시 실행됨)
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // 성공한 경우
                // result: SendResult 객체 (메타데이터 포함)
                log.info("✅ Message sent successfully:");
                log.info("   - Topic: {}", topicName);
                log.info("   - Key: {}", user);
                log.info("   - Partition: {}", result.getRecordMetadata().partition());
                log.info("   - Offset: {}", result.getRecordMetadata().offset());
                log.info("   - Timestamp: {}", result.getRecordMetadata().timestamp());
            } else {
                // 실패한 경우
                // ex: Exception 객체
                log.error("❌ Failed to send message:");
                log.error("   - Topic: {}", topicName);
                log.error("   - Key: {}", user);
                log.error("   - Error: {}", ex.getMessage(), ex);

                // 필요시 재시도, DLQ 전송 등 추가 처리
            }
        });

        // 3. 이 메서드는 즉시 반환됨 (논블로킹)
        // 전송 결과는 나중에 콜백에서 처리
    }

    /**
     * ===== 방법 3: 특정 파티션으로 전송 =====
     *
     * 동작:
     * 1. 파티션 번호를 직접 지정
     * 2. Partitioner를 거치지 않고 지정된 파티션으로 전송
     *
     * 사용 케이스:
     * - 특정 파티션에 데이터를 몰아서 보내고 싶을 때
     * - 파티션별로 다른 Consumer가 처리해야 할 때
     *
     * 주의:
     * - 파티션 번호는 0부터 시작
     * - 존재하지 않는 파티션 번호 사용 시 에러
     */
    public void sendToPartition(String user, String message, int partition) {
        String topicName = kafkaProperties.getTopics().get("chat").getName();

        // 파티션 번호를 명시적으로 지정해서 전송
        kafkaTemplate.send(topicName, partition, user, message)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Message sent to partition {}:", partition);
                        log.info("   - Offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send to partition {}: {}",
                                partition, ex.getMessage(), ex);
                    }
                });
    }
}

package com.example.kafka.service;

import com.example.kafka.dto.LogEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


/**
 * 로깅 서비스
 *
 * 역할:
 * 1. 구조화된 로그 생성
 * 2. Kafka로 로그 전송 (logback-kafka-appender 사용)
 * 3. 직접 Kafka Producer로 전송 (선택사항)
 *
 * 구조화된 로깅이란?
 * - 로그를 단순 문자열이 아닌 JSON 형태로 저장
 * - Elasticsearch에서 필드별 검색 가능
 * - 대시보드 생성 및 분석에 최적화
 *
 * 예시:
 * 비구조화: "User 홍길동 logged in from 192.168.1.1"
 * 구조화: {"userId": 123, "userName": "홍길동", "clientIp": "192.168.1.1", "action": "login"}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {
    // KafkaTemplate: 메시지를 Kafka로 전송하는 API
    // Spring Boot가 자동으로 생성해서 주입해줌
    private final KafkaTemplate<String, String> kafkaTemplate;

    // ObjectMapper: Java 객체 ↔ JSON 변환
    private final ObjectMapper objectMapper;

    // application.yml에서 주입
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 방법 1: Logback을 통한 구조화된 로깅 (권장!)
     *
     * 동작:
     * 1. log.info()로 로그 생성
     * 2. Logback이 로그 캡처
     * 3. logback-spring.xml의 KAFKA_APPENDER가 작동
     * 4. JSON으로 변환 후 Kafka의 "app-logs" 토픽으로 전송
     * 5. Logstash가 Kafka에서 읽어서 Elasticsearch로 전송
     *
     * 장점:
     * - 기존 로깅 방식 그대로 사용
     * - 모든 로그가 자동으로 Kafka로 전송
     * - 비동기 전송으로 성능 저하 없음
     *
     * StructuredArguments:
     * - Logstash JSON 인코더를 위한 헬퍼 클래스
     * - keyValue(): Key-Value 쌍으로 로그에 추가
     * - entries(): Map을 펼쳐서 로그에 추가
     */
    public void logStructured(Long userId, String userName, String action) {
        log.info("User action: {}",
                action,
                StructuredArguments.keyValue("userId", userId),
                StructuredArguments.keyValue("userName", userName),
                StructuredArguments.keyValue("action", action),
                StructuredArguments.keyValue("timestamp", LocalDateTime.now())
        );
    }

    /**
     * 방법 2: 직접 Kafka Producer로 전송
     *
     * 동작:
     * 1. LogEvent 객체 생성
     * 2. ObjectMapper로 JSON 문자열 변환
     * 3. KafkaTemplate.send()로 직접 Kafka 전송
     *
     * 장점:
     * - 토픽을 동적으로 선택 가능
     * - 로그 레벨과 무관하게 전송 가능
     * - 더 세밀한 제어 가능
     *
     * 단점:
     * - 코드가 약간 복잡
     * - 에러 처리 필요
     *
     * 사용 케이스:
     * - 비즈니스 이벤트 (주문, 결제 등)
     * - 특정 토픽으로 전송 필요 시
     * - 로그와 별개로 이벤트 발행
     */
    public void sendToKafka(String topic, LogEvent event) {
        try {
            // LogEvent 객체를 JSON 문자열로 변환
            String jsonMessage = objectMapper.writeValueAsString(event);

            // Kafka로 전송
            // topic: 대상 토픽
            // event.getUserId().toString(): 메시지 Key (파티셔닝 기준)
            // jsonMessage: 메시지 Value (실제 내용)
            kafkaTemplate.send(topic, event.getUserId().toString(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            // 전송 성공
                            log.debug("✅ Event sent to Kafka: topic={}, partition={}, offset={}",
                                    topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            // 전송 실패
                            log.error("❌ Failed to send event to Kafka: topic={}, error={}",
                                    topic, ex.getMessage(), ex);
                        }
                    });

        } catch (JsonProcessingException e) {
            // JSON 변환 실패
            log.error("Failed to serialize LogEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * 비즈니스 이벤트 로깅
     *
     * 사용 케이스:
     * - 주문 생성: ORDER_CREATED
     * - 결제 완료: PAYMENT_COMPLETED
     * - 회원 가입: USER_REGISTERED
     * - 로그인 성공: USER_LOGIN
     *
     * ELK에서 활용:
     * - 이벤트 타입별 통계
     * - 시간대별 이벤트 발생 추이
     * - 사용자별 이벤트 추적
     */
    public void logBusinessEvent(String eventType, Map<String, Object> eventData) {
        log.info("Business event: {}",
                eventType,
                StructuredArguments.keyValue("eventType", eventType),
                StructuredArguments.entries(eventData)
        );
    }

    /**
     * 에러 로깅 (스택 트레이스 포함)
     *
     * 동작:
     * 1. 에러 정보를 구조화된 형태로 로깅
     * 2. logback-spring.xml의 KAFKA_ERROR_APPENDER가 작동
     * 3. "error-logs" 토픽으로 전송
     *
     * ELK에서 활용:
     * - 에러 발생 추이 모니터링
     * - 특정 에러 타입 알람
     * - 스택 트레이스 분석
     */
    public void logError(String operation, Exception e) {
        Map<String, Object> errorContext = new HashMap<>();
        errorContext.put("operation", operation);
        errorContext.put("errorMessage", e.getMessage());
        errorContext.put("errorClass", e.getClass().getName());
        errorContext.put("timestamp", LocalDateTime.now());

        log.error("Operation failed: {}",
                operation,
                StructuredArguments.entries(errorContext),
                e  // 스택 트레이스
        );
    }

    /**
     * 성능 메트릭 로깅
     *
     * 사용 케이스:
     * - API 응답 시간 측정
     * - DB 쿼리 실행 시간
     * - 외부 API 호출 시간
     *
     * ELK에서 활용:
     * - 평균 응답 시간 그래프
     * - 느린 API 엔드포인트 파악
     * - 성능 저하 알람
     */
    public void logPerformance(String operation, long durationMs) {
        log.info("Performance metric",
                StructuredArguments.keyValue("operation", operation),
                StructuredArguments.keyValue("durationMs", durationMs),
                StructuredArguments.keyValue("metricType", "performance"),
                StructuredArguments.keyValue("timestamp", LocalDateTime.now())
        );
    }

    /**
     * 사용자 활동 추적
     *
     * 사용 케이스:
     * - 페이지 방문
     * - 버튼 클릭
     * - 검색어 입력
     * - 파일 다운로드
     *
     * ELK에서 활용:
     * - 사용자 행동 패턴 분석
     * - 인기 페이지/기능 파악
     * - 사용자 여정(User Journey) 추적
     */
    public void trackUserActivity(Long userId, String activityType, Map<String, Object> details) {
        Map<String, Object> activity = new HashMap<>(details);
        activity.put("userId", userId);
        activity.put("activityType", activityType);
        activity.put("timestamp", LocalDateTime.now());

        log.info("User activity tracked",
                StructuredArguments.entries(activity)
        );
    }
}

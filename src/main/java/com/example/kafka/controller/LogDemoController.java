package com.example.kafka.controller;

import com.example.kafka.config.KafkaProperties;
import com.example.kafka.dto.LogEvent;
import com.example.kafka.service.KafkaProducerService;
import com.example.kafka.service.LoggingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 로그 데모 컨트롤러
 *
 * 역할:
 * - 다양한 로그 생성 API 제공
 * - 접근 로그 자동 생성
 * - Kafka + ELK 스택 테스트
 *
 * 테스트 방법:
 * 1. 각 API 호출
 * 2. Kafka UI에서 메시지 확인 (http://localhost:8090)
 * 3. Kibana에서 로그 확인 (http://localhost:5601)
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class LogDemoController {

    private final LoggingService loggingService;
    private final KafkaProducerService kafkaProducerService;  // ✅ 추가!
    private final KafkaProperties kafkaProperties;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 1. 간단한 로그 생성
     *
     * GET /api/demo/simple-log
     *
     * 동작:
     * 1. 간단한 INFO 로그 생성
     * 2. Logback → Kafka → Logstash → Elasticsearch
     * 3. Kibana에서 확인 가능
     */
    @GetMapping("/simple-log")
    public ResponseEntity<String> simpleLog() {
        log.info("Simple log message generated");
        return ResponseEntity.ok("Simple log created!");
    }

    /**
     * 2. 구조화된 로그 생성
     *
     * GET /api/demo/structured-log?userId=123&userName=홍길동
     *
     * 동작:
     * 1. 구조화된 로그 생성 (JSON 형태)
     * 2. userId, userName 등을 별도 필드로 저장
     * 3. Kibana에서 필드별 검색 가능
     */
    @GetMapping("/structured-log")
    public ResponseEntity<String> structuredLog(
            @RequestParam Long userId,
            @RequestParam String userName) {

        // 구조화된 로그 생성
        loggingService.logStructured(userId, userName, "DEMO_ACTION");

        return ResponseEntity.ok("Structured log created!");
    }

    /**
     * 3. 비즈니스 이벤트 로그
     *
     * POST /api/demo/business-event
     * {
     *   "eventType": "ORDER_CREATED",
     *   "userId": 123,
     *   "orderId": 456,
     *   "amount": 10000
     * }
     *
     * 동작:
     * 1. 비즈니스 이벤트 로그 생성
     * 2. 이벤트 데이터를 구조화하여 저장
     * 3. ELK에서 이벤트 타입별 통계 가능
     */
    @PostMapping("/business-event")
    public ResponseEntity<Map<String, String>> businessEvent(
            @RequestBody Map<String, Object> eventData) {

        String eventType = eventData.getOrDefault("eventType", "UNKNOWN").toString();

        // 비즈니스 이벤트 로깅
        loggingService.logBusinessEvent(eventType, eventData);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Business event logged");

        return ResponseEntity.ok(response);
    }

    /**
     * 4. 에러 로그 생성
     *
     * GET /api/demo/error-log
     *
     * 동작:
     * 1. 의도적으로 에러 발생
     * 2. 에러 로그가 "error-logs" 토픽으로 전송
     * 3. Kibana에서 에러 모니터링 가능
     */
    @GetMapping("/error-log")
    public ResponseEntity<String> errorLog() {
        try {
            // 의도적으로 에러 발생
            throw new RuntimeException("This is a test error for ELK stack");
        } catch (Exception e) {
            // 에러 로깅
            loggingService.logError("errorLogTest", e);

            return ResponseEntity.internalServerError()
                    .body("Error logged! Check Kibana for details.");
        }
    }

    /**
     * 5. 성능 메트릭 로그
     *
     * GET /api/demo/performance-log
     *
     * 동작:
     * 1. API 실행 시간 측정
     * 2. 응답 시간을 로그로 저장
     * 3. Kibana에서 성능 대시보드 생성 가능
     */
    @GetMapping("/performance-log")
    public ResponseEntity<String> performanceLog() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // 작업 시뮬레이션 (200ms 대기)
        Thread.sleep(200);

        long duration = System.currentTimeMillis() - startTime;

        // 성능 로깅
        loggingService.logPerformance("performanceLogTest", duration);

        return ResponseEntity.ok("Performance metric logged! Duration: " + duration + "ms");
    }

    /**
     * 6. 사용자 활동 추적
     *
     * POST /api/demo/track-activity
     * {
     *   "userId": 123,
     *   "activityType": "PAGE_VIEW",
     *   "pageName": "/products/12345",
     *   "device": "mobile"
     * }
     *
     * 동작:
     * 1. 사용자 활동 데이터 수집
     * 2. 구조화된 로그로 저장
     * 3. ELK에서 사용자 행동 분석
     */
    @PostMapping("/track-activity")
    public ResponseEntity<String> trackActivity(
            @RequestBody Map<String, Object> activityData) {

        Long userId = Long.parseLong(activityData.get("userId").toString());
        String activityType = activityData.get("activityType").toString();

        // userId와 activityType 제거 (중복 방지)
        Map<String, Object> details = new HashMap<>(activityData);
        details.remove("userId");
        details.remove("activityType");

        // 사용자 활동 추적
        loggingService.trackUserActivity(userId, activityType, details);

        return ResponseEntity.ok("Activity tracked!");
    }

    /**
     * 7. 직접 Kafka로 이벤트 전송
     *
     * POST /api/demo/send-event
     * {
     *   "userId": 123,
     *   "userName": "홍길동",
     *   "action": "LOGIN"
     * }
     *
     * 동작:
     * 1. LogEvent 객체 생성
     * 2. KafkaTemplate으로 직접 Kafka 전송
     * 3. "business-events" 토픽으로 전송
     * 4. Logstash → Elasticsearch → Kibana
     */
    @PostMapping("/send-event")
    public ResponseEntity<String> sendEvent(
            @RequestBody Map<String, Object> requestData,
            HttpServletRequest httpRequest) {

        // LogEvent 생성
        LogEvent event = LogEvent.builder()
                .eventType("DIRECT_KAFKA_EVENT")
                .userId(Long.parseLong(requestData.get("userId").toString()))
                .userName(requestData.get("userName").toString())
                .clientIp(getClientIp(httpRequest))
                .httpMethod(httpRequest.getMethod())
                .endpoint(httpRequest.getRequestURI())
                .additionalData(requestData)
                .timestamp(LocalDateTime.now())
                .application(applicationName)
                .environment("development")
                .build();

        // Kafka로 직접 전송
        String topic = kafkaProperties.getTopics().get("business-events").getName();
        loggingService.sendToKafka(topic, event);

        return ResponseEntity.ok("Event sent to Kafka directly!");
    }

    /**
     * 8. 대량 로그 생성 (부하 테스트)
     *
     * POST /api/demo/bulk-logs?count=100
     *
     * 동작:
     * 1. 지정된 개수만큼 로그 생성
     * 2. Kafka 처리량 테스트
     * 3. ELK 스택 부하 테스트
     */
    @PostMapping("/bulk-logs")
    public ResponseEntity<String> bulkLogs(@RequestParam(defaultValue = "100") int count) {
        log.info("Generating {} bulk logs...", count);

        for (int i = 0; i < count; i++) {
            loggingService.logStructured(
                    (long) i,
                    "User" + i,
                    "BULK_TEST_" + i
            );
        }

        return ResponseEntity.ok(count + " logs generated!");
    }

    /**
     * Kafka Producer Service를 직접 사용하는 예제
     *
     * POST /api/demo/direct-kafka
     * {
     *   "orderId": 123,
     *   "amount": 10000
     * }
     */
    @PostMapping("/direct-kafka")
    public ResponseEntity<String> directKafka(@RequestBody Map<String, Object> data) {

        String topic = kafkaProperties.getTopics().get("business-events").getName();
        String key = data.get("orderId").toString();

        // ✅ Producer Service 직접 사용
        kafkaProducerService.sendObject(topic, key, data);

        return ResponseEntity.ok("Event sent via Producer Service!");
    }

    /**
     * 클라이언트 IP 추출
     *
     * Proxy나 Load Balancer를 거치는 경우를 고려
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}

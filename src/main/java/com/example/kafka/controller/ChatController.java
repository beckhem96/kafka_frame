package com.example.kafka.controller;

import com.example.kafka.dto.ChatRequest;
import com.example.kafka.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 채팅 API 컨트롤러
 *
 * 역할:
 * - 사용자의 HTTP 요청을 받음
 * - 요청 데이터 검증
 * - ProducerService를 호출해 Kafka로 메시지 전송
 * - 응답 반환
 *
 * 흐름:
 * 사용자 → HTTP POST /api/chat
 *       ↓
 * Controller: 요청 수신 및 검증
 *       ↓
 * ProducerService: Kafka로 전송
 *       ↓
 * Controller: 응답 반환
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    // Kafka Producer 서비스 주입
    private final KafkaProducerService producerService;

    /**
     * ===== API 1: 채팅 메시지 전송 (비동기) =====
     *
     * Endpoint: POST /api/chat
     * Request Body: {"user": "홍길동", "message": "안녕하세요"}
     *
     * 동작 순서:
     * 1. 사용자가 POST 요청 전송
     * 2. @Validated로 ChatRequest 검증
     *    - user: 2-20자 필수
     *    - message: 1-500자 필수
     * 3. 검증 통과 시 ProducerService.sendAsync() 호출
     * 4. 즉시 응답 반환 (비동기이므로 전송 완료를 기다리지 않음)
     * 5. 백그라운드에서 Kafka 전송 진행
     *
     * 응답:
     * 200 OK {"status": "success", "message": "메시지가 전송되었습니다"}
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> sendMessage(@Validated @RequestBody ChatRequest request) {

        // 1. 요청 로깅
        log.info("📨 Received chat message:");
        log.info("   - User: {}", request.getUser());
        log.info("   - Message: {}", request.getMessage());

        // 2. Kafka로 메시지 전송 (비동기)
        // 이 메서드는 즉시 반환됨
        producerService.sendAsync(request.getUser(), request.getMessage());

        // 3. 응답 생성
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "메시지가 전송되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * ===== API 2: 채팅 메시지 전송 (동기) =====
     *
     * Endpoint: POST /api/chat/sync
     * Request Body: {"user": "김철수", "message": "테스트"}
     *
     * 동작 순서:
     * 1. 사용자가 POST 요청 전송
     * 2. ChatRequest 검증
     * 3. ProducerService.sendSync() 호출
     * 4. Kafka 전송 완료까지 대기 (블로킹)
     * 5. 전송 성공/실패 결과 확인 후 응답
     *
     * 차이점:
     * - 비동기: 즉시 응답, 빠름
     * - 동기: 전송 완료 후 응답, 느림, 확실함
     *
     * 사용 케이스:
     * - 테스트
     * - 중요한 메시지 (반드시 전송 확인 필요)
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sendMessageSync(@Validated @RequestBody ChatRequest request) {
        log.info("Received chat message (sync):");
        log.info("   - User: {}", request.getUser());
        log.info("   - Message: {}", request.getMessage());

        try {
            // 동기 전송 (전송 완료까지 대기)
            producerService.sendSync(request.getUser(), request.getMessage());

            // 전송 성공
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "메시지가 전송되었습니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 전송 실패
            log.error("Failed to send message", e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "메시지 전송 실패: " + e.getMessage());

            // HTTP 500 Internal Server Error
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * ===== API 3: 특정 파티션으로 전송 =====
     *
     * Endpoint: POST /api/chat/partition/{partition}
     * Path Variable: partition (파티션 번호)
     * Request Body: {"user": "이영희", "message": "파티션 0"}
     *
     * 동작:
     * - 지정된 파티션으로 메시지 전송
     * - 일반적으로는 Key 기반으로 자동 분배되지만
     *   특정 파티션을 지정하고 싶을 때 사용
     *
     * 예시:
     * POST /api/chat/partition/0
     * → 파티션 0으로 전송
     */
    @PostMapping("/partition/{partition}")  // POST /api/chat/partition/0
    public ResponseEntity<Map<String, String>> sendToPartition(
            @PathVariable int partition,  // URL에서 파티션 번호 추출
            @Validated @RequestBody ChatRequest request) {

        log.info("📨 Sending message to partition {}:", partition);
        log.info("   - User: {}", request.getUser());
        log.info("   - Message: {}", request.getMessage());

        // 특정 파티션으로 전송
        producerService.sendToPartition(
                request.getUser(),
                request.getMessage(),
                partition
        );

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "메시지가 파티션 " + partition + "으로 전송되었습니다");

        return ResponseEntity.ok(response);
    }

    /**
     * ===== API 4: Health Check =====
     *
     * Endpoint: GET /api/chat/health
     *
     * 동작:
     * - 서비스가 정상 작동하는지 확인
     * - 모니터링 시스템에서 주기적으로 호출
     *
     * 응답:
     * 200 OK {"status": "UP", "service": "Kafka Chat Service"}
     */
    @GetMapping("/health")  // GET /api/chat/health
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Kafka Chat Service");

        return ResponseEntity.ok(response);
    }
}

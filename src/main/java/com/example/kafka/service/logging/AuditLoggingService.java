package com.example.kafka.service.logging;

import com.example.kafka.dto.AuditEvent;
import com.example.kafka.constants.LogMarkers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * 감사 로깅 서비스
 *
 * 역할:
 * - 규정 준수를 위한 감사 로그 생성
 * - 모든 중요 작업 기록
 * - 7년 보관
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLoggingService {
    private final ObjectMapper objectMapper;

    /**
     * 감사 로그 기록
     *
     * 사용:
     * auditLoggingService.log(AuditEvent.builder()
     *     .userId("user123")
     *     .action("ORDER_CREATE")
     *     .resource("orders")
     *     .resourceId("order-456")
     *     .result("SUCCESS")
     *     .build());
     */
    public void log(AuditEvent event) {
        try {
            // MDC에 감사 정보 추가
            MDC.put("userId", event.getUserId());
            MDC.put("action", event.getAction());
            MDC.put("resource", event.getResource());
            MDC.put("result", event.getResult());

            // JSON 직렬화
            String eventJson = objectMapper.writeValueAsString(event);

            // ✅ AUDIT 마커로 로깅 → audit-logs 토픽
            log.info(LogMarkers.AUDIT, "Audit event: {}", eventJson);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit event", e);
        } finally {
            // MDC 정리
            MDC.remove("userId");
            MDC.remove("action");
            MDC.remove("resource");
            MDC.remove("result");
        }
    }

    /**
     * 간편 감사 로그
     */
    public void log(String userId, String action, String resource, String resourceId, String result) {
        AuditEvent event = AuditEvent.builder()
                .userId(userId)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .result(result)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        log(event);
    }
}

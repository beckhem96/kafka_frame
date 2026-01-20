package com.example.kafka.service;

import com.example.kafka.constants.LogMarkers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 주문 서비스
 *
 * Logback을 통해 모든 로그가 자동으로 Kafka로 전송됨
 * KafkaTemplate 불필요!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    /**
     * 주문 생성
     *
     * 이 메서드는 다음과 같은 로그를 생성합니다:
     * 1. 일반 애플리케이션 로그 (INFO) → app-logs-order-service
     * 2. 감사 로그 (AUDIT 마커) → audit-logs
     * 3. 비즈니스 이벤트 (BUSINESS 마커) → business-events-order-service
     * 4. 에러 로그 (ERROR) → error-logs-order-service
     */
    public String createOrder(String userId, String productId, Double amount) {
        String orderId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();

        try {
            // ===== MDC 설정 (모든 로그에 자동 포함) =====
            MDC.put("traceId", traceId);
            MDC.put("userId", userId);
            MDC.put("orderId", orderId);

            // ===== 1. 일반 애플리케이션 로그 =====
            // → app-logs-order-service 토픽
            log.info("Starting order creation for user: {}, product: {}, amount: {}",
                    userId, productId, amount);

            // ===== 2. 감사 로그 (AUDIT 마커) =====
            // → audit-logs 토픽 (통합!)
            MDC.put("action", "ORDER_CREATE");
            MDC.put("resource", "order:" + orderId);
            log.info(LogMarkers.AUDIT,
                    "User {} created order {} with amount {}",
                    userId, orderId, amount);

            // 비즈니스 로직 시뮬레이션
            processOrder(orderId, productId);

            // ===== 3. 비즈니스 이벤트 =====
            // → business-events-order-service 토픽
            log.info(LogMarkers.BUSINESS,
                    "Order created successfully: orderId={}", orderId);

            log.info("Order creation completed: {}", orderId);

            return orderId;

        } catch (Exception e) {
            // ===== 5. 에러 로그 =====
            // → error-logs-order-service 토픽
            log.error("Failed to create order for user: {}", userId, e);
            throw new RuntimeException("Order creation failed", e);

        } finally {
            MDC.clear();  // MDC 정리
        }
    }

    /**
     * 주문 취소
     *
     * 감사 로그와 비즈니스 이벤트 예시
     */
    public void cancelOrder(String orderId, String userId, String reason) {
        String traceId = UUID.randomUUID().toString();

        try {
            MDC.put("traceId", traceId);
            MDC.put("userId", userId);
            MDC.put("orderId", orderId);

            log.info("Cancelling order: {}, reason: {}", orderId, reason);

            // ===== 감사 로그 =====
            MDC.put("action", "ORDER_CANCEL");
            MDC.put("resource", "order:" + orderId);
            log.info(LogMarkers.AUDIT,
                    "User {} cancelled order {} - reason: {}",
                    userId, orderId, reason);

            // ===== 비즈니스 이벤트 =====
            log.info(LogMarkers.BUSINESS,
                    "Order {} cancelled by user {}",
                    orderId, userId);

        } finally {
            MDC.clear();
        }
    }

    /**
     * 인증 실패 (보안 로그 예시)
     */
    public void logAuthenticationFailure(String userId, String ipAddress) {
        try {
            MDC.put("userId", userId);
            MDC.put("ipAddress", ipAddress);

            // ===== 보안 로그 (SECURITY 마커) =====
            // → security-logs 토픽 (통합!)
            log.warn(LogMarkers.SECURITY,
                    "Authentication failed for user {} from IP {}",
                    userId, ipAddress);

        } finally {
            MDC.clear();
        }
    }

    /**
     * 성능 측정 (메트릭 로그 예시)
     */
    public void measurePerformance(String operation, long durationMs) {
        try {
            MDC.put("operation", operation);
            MDC.put("duration", String.valueOf(durationMs));

            // ===== 메트릭 로그 (METRIC 마커) =====
            // → metric-logs-order-service 토픽
            log.info(LogMarkers.METRIC,
                    "Operation {} completed in {}ms",
                    operation, durationMs);

        } finally {
            MDC.clear();
        }
    }

    /**
     * 비즈니스 로직 시뮬레이션
     */
    private void processOrder(String orderId, String productId) {
        // 재고 확인, 결제 등...
        log.debug("Processing order: {}, product: {}", orderId, productId);
    }
}

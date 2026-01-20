package com.example.kafka.controller;

import com.example.kafka.dto.OrderRequest;
import com.example.kafka.service.OrderService;
import com.example.kafka.constants.LogMarkers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
* 주문 API 컨트롤러
*
* 접근 로그(ACCESS) 예시*/
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성 API
     *
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // MDC 설정
            MDC.put("requestId", requestId);
            MDC.put("userId", userId != null ? userId : "anonymous");
            MDC.put("endpoint", "POST /api/orders");

            // ===== 접근 로그 (ACCESS 마커) =====
            // → access-logs-order-service 토픽
            log.info(LogMarkers.ACCESS,
                    "Received order creation request from user: {}, product: {}",
                    userId, request.getProductId());

            // 비즈니스 로직 호출
            String orderId = orderService.createOrder(
                    userId,
                    request.getProductId(),
                    request.getAmount()
            );

            long duration = System.currentTimeMillis() - startTime;

            // ===== 성공 접근 로그 =====
            MDC.put("statusCode", "200");
            MDC.put("duration", String.valueOf(duration));
            log.info(LogMarkers.ACCESS,
                    "Order creation request completed: orderId={}, duration={}ms",
                    orderId, duration);

            Map<String, String> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // ===== 실패 접근 로그 =====
            MDC.put("statusCode", "500");
            MDC.put("duration", String.valueOf(duration));
            log.error(LogMarkers.ACCESS,
                    "Order creation request failed: duration={}ms",
                    duration, e);

            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));

        } finally {
            MDC.clear();
        }
    }

    /**
     * 주문 취소 API
     *
     * DELETE /api/orders/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, String>> cancelOrder(
            @PathVariable String orderId,
            @RequestParam String reason,
            @RequestHeader(value = "X-User-Id") String userId) {

        try {
            MDC.put("userId", userId);
            MDC.put("orderId", orderId);
            MDC.put("endpoint", "DELETE /api/orders/{orderId}");

            log.info(LogMarkers.ACCESS,
                    "Received order cancellation request: orderId={}, reason={}",
                    orderId, reason);

            orderService.cancelOrder(orderId, userId, reason);

            return ResponseEntity.ok(Map.of("status", "cancelled"));

        } finally {
            MDC.clear();
        }
    }

    /**
     * 에러 발생 API
     *
     * GET /api/orders/error
     */
    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> errorOrder() {

        try {
            MDC.put("userId", "test");
            MDC.put("orderId", "1");
            MDC.put("endpoint", "GET /api/orders/error");

            log.error("test용 에러입니다만");

            return ResponseEntity.ok(Map.of("status", "error"));

        } finally {
            MDC.clear();
        }
    }
}
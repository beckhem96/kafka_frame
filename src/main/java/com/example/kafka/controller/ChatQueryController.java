package com.example.kafka.controller;

import com.example.kafka.dto.ChatMessageResponse;
import com.example.kafka.service.ChatQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 메시지 조회 API
 *
 * 역할:
 * - Cassandra에서 저장된 채팅 조회
 * - 다양한 조회 조건 지원
 *
 * Endpoint:
 * - GET /api/chat/users/{userName}/messages
 * - GET /api/chat/users/{userName}/messages/recent
 * - GET /api/chat/users/{userName}/messages/range
 * - GET /api/chat/users/{userName}/messages/today
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatQueryController {
    private final ChatQueryService chatQueryService;

    /**
     * 사용자의 모든 메시지 조회
     *
     * GET /api/chat/users/홍길동/messages
     *
     * 응답:
     * [
     *   {
     *     "messageId": "uuid...",
     *     "userName": "홍길동",
     *     "message": "안녕하세요",
     *     "createdAt": "2024-01-13 10:30:00"
     *   },
     *   ...
     * ]
     */
    @GetMapping("/users/{userName}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getUserMessages(
            @PathVariable String userName) {

        log.info("📨 Request: Get all messages for user: {}", userName);

        List<ChatMessageResponse> messages =
                chatQueryService.getUserMessages(userName);

        return ResponseEntity.ok(messages);
    }

    /**
     * 사용자의 최근 N개 메시지 조회
     *
     * GET /api/chat/users/홍길동/messages/recent?limit=100
     *
     * Query Parameter:
     * - limit: 조회할 메시지 개수 (기본 100)
     */
    @GetMapping("/users/{userName}/messages/recent")
    public ResponseEntity<List<ChatMessageResponse>> getRecentMessages(
            @PathVariable String userName,
            @RequestParam(defaultValue = "100") int limit) {

        log.info("📨 Request: Get recent {} messages for user: {}",
                limit, userName);

        List<ChatMessageResponse> messages =
                chatQueryService.getRecentUserMessages(userName, limit);

        return ResponseEntity.ok(messages);
    }
    /**
     * 특정 기간의 메시지 조회
     *
     * GET /api/chat/users/홍길동/messages/range
     *     ?startDate=2024-01-01T00:00:00
     *     &endDate=2024-01-31T23:59:59
     *
     * Query Parameters:
     * - startDate: 시작 시간 (ISO 8601 형식)
     * - endDate: 종료 시간 (ISO 8601 형식)
     */
    @GetMapping("/users/{userName}/messages/range")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesInRange(
            @PathVariable String userName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {

        log.info("📨 Request: Get messages for user: {} between {} and {}",
                userName, startDate, endDate);

        List<ChatMessageResponse> messages =
                chatQueryService.getUserMessagesInRange(userName, startDate, endDate);

        return ResponseEntity.ok(messages);
    }

    /**
     * 오늘의 메시지 조회
     *
     * GET /api/chat/users/홍길동/messages/today
     */
    @GetMapping("/users/{userName}/messages/today")
    public ResponseEntity<List<ChatMessageResponse>> getTodayMessages(
            @PathVariable String userName) {

        log.info("📨 Request: Get today's messages for user: {}", userName);

        List<ChatMessageResponse> messages =
                chatQueryService.getTodayUserMessages(userName);

        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 시점 이후 메시지 조회
     *
     * GET /api/chat/users/홍길동/messages/since
     *     ?since=2024-01-13T10:00:00
     *
     * 사용 케이스:
     * - 실시간 채팅에서 새 메시지 polling
     */
    @GetMapping("/users/{userName}/messages/since")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesSince(
            @PathVariable String userName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since) {

        log.info("📨 Request: Get messages for user: {} since {}",
                userName, since);

        List<ChatMessageResponse> messages =
                chatQueryService.getUserMessagesSince(userName, since);

        return ResponseEntity.ok(messages);
    }

}

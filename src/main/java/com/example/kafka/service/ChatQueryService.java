package com.example.kafka.service;

import com.example.kafka.dto.ChatMessageResponse;
import com.example.kafka.entity.ChatMessageByUser;
import com.example.kafka.repository.ChatMessageByUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 메시지 조회 서비스
 *
 * 역할:
 * - Cassandra에서 채팅 메시지 조회
 * - Entity → DTO 변환
 *
 * Cassandra 조회 최적화 팁:
 * 1. 항상 Partition Key 포함 (WHERE user_name = ?)
 * 2. Clustering Key로 범위 검색 (WHERE created_at > ?)
 * 3. LIMIT 사용으로 데이터 제한
 * 4. 페이징은 PagingState 사용 (offset 방식 비효율적)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatQueryService {
    private final ChatMessageByUserRepository messageByUserRepository;

    /**
     * 사용자의 모든 채팅 조회
     *
     * 쿼리:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * ORDER BY created_at DESC
     *
     * 성능:
     * - 단일 파티션 스캔
     * - 매우 빠름 (밀리초 단위)
     *
     * 주의:
     * - 한 사용자의 메시지가 너무 많으면 느려질 수 있음
     * - 실무에서는 LIMIT 사용 권장
     */
    public List<ChatMessageResponse> getUserMessages(String userName) {
        log.info("🔍 Querying messages for user: {}", userName);

        List<ChatMessageByUser> messages =
                messageByUserRepository.findByUserName(userName);

        log.info("✅ Found {} messages for user: {}", messages.size(), userName);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 최근 N개 메시지 조회
     *
     * 쿼리:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * ORDER BY created_at DESC
     * LIMIT ?
     *
     * 사용 케이스:
     * - 채팅방 입장 시 최근 100개 표시
     * - 모바일 앱에서 스크롤 시 20개씩 로드
     */
    public List<ChatMessageResponse> getRecentUserMessages(
            String userName,
            int limit) {

        log.info("🔍 Querying recent {} messages for user: {}", limit, userName);

        List<ChatMessageByUser> messages =
                messageByUserRepository.findByUserNameOrderByCreatedAtDesc(
                        userName,
                        limit
                );

        log.info("✅ Found {} messages for user: {}", messages.size(), userName);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간의 메시지 조회
     *
     * 쿼리:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * AND created_at >= ?
     * AND created_at <= ?
     * ORDER BY created_at DESC
     *
     * 사용 케이스:
     * - 특정 날짜/주/월의 채팅 조회
     * - 기간별 통계
     */
    public List<ChatMessageResponse> getUserMessagesInRange(
            String userName,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        log.info("🔍 Querying messages for user: {} between {} and {}",
                userName, startDate, endDate);

        List<ChatMessageByUser> messages =
                messageByUserRepository.findByUserNameAndCreatedAtBetween(
                        userName,
                        startDate,
                        endDate
                );

        log.info("✅ Found {} messages in range", messages.size());

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시점 이후 메시지 조회
     *
     * 쿼리:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * AND created_at > ?
     * ORDER BY created_at DESC
     *
     * 사용 케이스:
     * - 실시간 채팅에서 새 메시지 polling
     * - "이후 메시지 더보기" 기능
     */
    public List<ChatMessageResponse> getUserMessagesSince(
            String userName,
            LocalDateTime since) {

        log.info("🔍 Querying messages for user: {} since {}", userName, since);

        List<ChatMessageByUser> messages =
                messageByUserRepository.findByUserNameAndCreatedAtGreaterThan(
                        userName,
                        since
                );

        log.info("✅ Found {} new messages", messages.size());

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 오늘의 메시지 조회
     *
     * 편의 메서드
     */
    public List<ChatMessageResponse> getTodayUserMessages(String userName) {
        LocalDateTime startOfDay = LocalDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        LocalDateTime endOfDay = LocalDateTime.now()
                .withHour(23)
                .withMinute(59)
                .withSecond(59);

        return getUserMessagesInRange(userName, startOfDay, endOfDay);
    }
}

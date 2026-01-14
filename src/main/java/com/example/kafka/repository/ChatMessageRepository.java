package com.example.kafka.repository;

import com.example.kafka.entity.ChatMessage;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * ChatMessage Repository
 *
 * CassandraRepository:
 * - Spring Data의 Repository 인터페이스
 * - 기본 CRUD 메서드 제공 (save, findById, delete 등)
 * - 커스텀 쿼리 메서드 작성 가능
 *
 * 주의사항:
 * - Cassandra는 조인 불가
 * - WHERE 절에 Partition Key 필수
 * - 집계 함수(COUNT, SUM) 사용 지양 (느림)
 */
@Repository
public interface ChatMessageRepository extends CassandraRepository<ChatMessage, UUID> {
    /**
     * 메시지 ID로 조회
     * - CassandraRepository가 기본 제공
     * - Partition Key로 조회하므로 매우 빠름
     *
     * 생성되는 쿼리:
     * SELECT * FROM chat_messages WHERE message_id = ?
     */
    // Optional<ChatMessage> findById(UUID messageId);  // 기본 제공

    /**
     * 최근 N개 메시지 조회
     *
     * 주의:
     * - LIMIT은 각 파티션에서 가져오는 개수
     * - 전체 데이터를 스캔하므로 느릴 수 있음
     * - 실무에서는 사용 지양
     */
    @Query("SELECT * FROM chat_messages LIMIT ?0")
    List<ChatMessage> findRecentMessages(int limit);
}

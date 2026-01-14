package com.example.kafka.repository;

import com.example.kafka.entity.ChatMessageByUser;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 사용자별 채팅 메시지 Repository
 *
 * 이 Repository의 모든 쿼리는:
 * - Partition Key (user_name)를 WHERE 절에 포함
 * - 단일 파티션 스캔으로 매우 빠름
 * - 실무에서 권장하는 패턴
 */
@Repository
public interface ChatMessageByUserRepository extends CassandraRepository<ChatMessageByUser, UUID> {
    /**
     * 사용자의 모든 메시지 조회 (최신순)
     *
     * 메서드 이름으로 쿼리 자동 생성:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * ORDER BY created_at DESC
     *
     * 특징:
     * - Partition Key로 조회하므로 매우 빠름
     * - Clustering Key의 DESCENDING 설정으로 자동 정렬
     */
    List<ChatMessageByUser> findByUserName(String userName);

    /**
     * 사용자의 최근 N개 메시지 조회
     *
     * Spring Data의 메서드 이름 규칙:
     * - findBy: SELECT
     * - UserName: WHERE user_name = ?
     * - OrderBy: ORDER BY
     * - CreatedAtDesc: created_at DESC
     * - Limit: LIMIT ?
     *
     * 생성되는 쿼리:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * ORDER BY created_at DESC
     * LIMIT ?
     */
    List<ChatMessageByUser> findByUserNameOrderByCreatedAtDesc(
            String userName,
            int limit
    );

    /**
     * 특정 기간의 메시지 조회
     *
     * Between: 범위 검색
     * Clustering Key로 범위 검색 가능
     *
     * 생성되는 쿼리:
     * SELECT * FROM chat_messages_by_user
     * WHERE user_name = ?
     * AND created_at >= ?
     * AND created_at <= ?
     * ORDER BY created_at DESC
     */
    List<ChatMessageByUser> findByUserNameAndCreatedAtBetween(
            String userName,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * 특정 시점 이후의 메시지 조회
     *
     * GreaterThan: >
     * GreaterThanEqual: >=
     * LessThan:
     * LessThanEqual: <=
     */
    List<ChatMessageByUser> findByUserNameAndCreatedAtGreaterThan(
            String userName,
            LocalDateTime startDate
    );

    /**
     * 커스텀 쿼리 (CQL 직접 작성)
     *
     * @Query: Cassandra Query Language (CQL) 직접 작성
     * ?0, ?1: 파라미터 순서
     *
     * 사용 케이스:
     * - 복잡한 쿼리
     * - 메서드 이름으로 표현 어려운 경우
     */
    @Query("SELECT * FROM chat_messages_by_user " +
            "WHERE user_name = ?0 " +
            "AND created_at >= ?1 " +
            "AND created_at <= ?2 " +
            "LIMIT ?3")
    List<ChatMessageByUser> findMessagesInRange(
            String userName,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int limit
    );
}

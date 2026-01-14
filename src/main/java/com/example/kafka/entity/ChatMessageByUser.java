package com.example.kafka.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자별 채팅 메시지 조회용 Entity
 *
 * Cassandra 설계 철학:
 * - "하나의 쿼리 패턴 = 하나의 테이블"
 * - 조인이 없으므로 쿼리 패턴마다 별도 테이블 생성
 *
 * 이 테이블의 목적:
 * - "특정 사용자의 채팅 이력 조회" 쿼리 최적화
 *
 * Primary Key 설계:
 * PRIMARY KEY ((user_name), created_at, message_id)
 * - Partition Key: user_name (사용자별로 데이터 분산)
 * - Clustering Key: created_at (시간순 정렬), message_id (고유성 보장)
 *
 * 가능한 쿼리:
 * ✅ SELECT * FROM chat_messages_by_user WHERE user_name = '홍길동'
 * ✅ SELECT * FROM chat_messages_by_user WHERE user_name = '홍길동'
 *    AND created_at > '2024-01-01' AND created_at < '2024-01-31'
 * ✅ SELECT * FROM chat_messages_by_user WHERE user_name = '홍길동' LIMIT 100
 *
 * 불가능한 쿼리:
 * ❌ SELECT * FROM chat_messages_by_user WHERE created_at > '2024-01-01'
 *    (Partition Key인 user_name이 없음)
 */
@Table("chat_messages_by_user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageByUser {
    /**
     * 사용자 이름 (Partition Key)
     * - 같은 사용자의 메시지는 같은 노드에 저장됨
     * - 사용자별 메시지 조회가 빠름
     */
    @PrimaryKeyColumn(
            name = "user_name",
            ordinal = 0,
            type = PrimaryKeyType.PARTITIONED
    )
    private String userName;

    /**
     * 생성 시간 (Clustering Key 1)
     * - 시간순 정렬
     * - 범위 검색 가능 (created_at > X AND created_at < Y)
     */
    @PrimaryKeyColumn(
            name = "created_at",
            ordinal = 1,
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING  // 최신순 정렬
    )
    private LocalDateTime createdAt;

    /**
     * 메시지 ID (Clustering Key 2)
     * - 같은 시간에 여러 메시지가 있을 때 고유성 보장
     * - UUID 사용으로 충돌 방지
     */
    @PrimaryKeyColumn(
            name = "message_id",
            ordinal = 2,
            type = PrimaryKeyType.CLUSTERED
    )
    private UUID messageId;

    /**
     * 메시지 내용
     */
    @Column("message")
    private String message;

    /**
     * Kafka 메타데이터
     */
    @Column("kafka_partition")
    private Integer kafkaPartition;

    @Column("kafka_offset")
    private Long kafkaOffset;

    /**
     * 팩토리 메서드
     */
    public static ChatMessageByUser from(String userName, String message,
                                         Integer kafkaPartition, Long kafkaOffset) {
        return ChatMessageByUser.builder()
                .userName(userName)
                .messageId(UUID.randomUUID())
                .message(message)
                .createdAt(LocalDateTime.now())
                .kafkaPartition(kafkaPartition)
                .kafkaOffset(kafkaOffset)
                .build();
    }
}

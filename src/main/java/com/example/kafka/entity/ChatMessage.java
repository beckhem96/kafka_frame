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
 * 채팅 메시지 Entity (Cassandra)
 *
 * Cassandra 테이블 설계 원칙:
 * 1. 쿼리 패턴에 맞게 설계 (Query-First Design)
 * 2. 비정규화 (중복 허용)
 * 3. Partition Key 선택이 중요 (데이터 분산)
 *
 * Primary Key 구조:
 * - Partition Key: 데이터를 어느 노드에 저장할지 결정
 * - Clustering Key: 같은 파티션 내 정렬 기준
 *
 * 예시:
 * PRIMARY KEY ((user), created_at)
 * - Partition Key: user (사용자별로 데이터 분산)
 * - Clustering Key: created_at (시간순 정렬)
 *
 * 쿼리 가능:
 * ✅ SELECT * FROM chat_messages WHERE user = '홍길동'
 * ✅ SELECT * FROM chat_messages WHERE user = '홍길동' AND created_at > '2024-01-01'
 * ❌ SELECT * FROM chat_messages WHERE created_at > '2024-01-01' (Partition Key 필수!)
 */

@Table("chat_messages") // Cassandra 테이블 이름
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    /**
     * 메시지 ID (UUID)
     * - Cassandra에서 UUID 사용이 일반적
     * - 분산 환경에서 충돌 없이 고유 ID 생성 가능
     */
    @PrimaryKeyColumn(
            name = "message_id",
            ordinal = 0,
            type = PrimaryKeyType.PARTITIONED  // Partition Key
    )
    private UUID messageId;

    /**
     * 사용자 이름
     * - 보조 인덱스로 검색 가능하게 설정
     * - 실무에서는 user_id (UUID)를 사용하는 것이 더 좋음
     */
    @Column("user_name")
    private String userName;

    /**
     * 메시지 내용
     */
    @Column("message")
    private String message;

    /**
     * 생성 시간
     * - Clustering Key로 사용하면 시간순 정렬 가능
     */
    @PrimaryKeyColumn(
            name = "created_at",
            ordinal = 1,
            type = PrimaryKeyType.CLUSTERED,  // Clustering Key
            ordering = Ordering.DESCENDING    // 내림차순 정렬 (최신순)
    )
    private LocalDateTime createdAt;

    /**
     * Kafka 메타데이터 (선택사항)
     */
    @Column("kafka_partition")
    private Integer kafkaPartition;

    @Column("kafka_offset")
    private Long kafkaOffset;

    /**
     * 팩토리 메서드
     * Kafka Consumer에서 받은 데이터로 Entity 생성
     */
    public static ChatMessage from(String userName, String message,
                                   Integer kafkaPartition, Long kafkaOffset) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID())  // UUID 자동 생성
                .userName(userName)
                .message(message)
                .createdAt(LocalDateTime.now())
                .kafkaPartition(kafkaPartition)
                .kafkaOffset(kafkaOffset)
                .build();
    }
}

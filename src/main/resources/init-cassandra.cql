-- 파일명: init-cassandra.cql

-- Keyspace 생성
CREATE KEYSPACE IF NOT EXISTS chat_keyspace
    WITH replication = {
        'class': 'SimpleStrategy',
        'replication_factor': 1
        };

-- Keyspace 사용
USE chat_keyspace;

-- 테이블 1: 메시지 ID로 조회
CREATE TABLE IF NOT EXISTS chat_messages (
                                             message_id UUID,
                                             user_name TEXT,
                                             message TEXT,
                                             created_at TIMESTAMP,
                                             kafka_partition INT,
                                             kafka_offset BIGINT,
                                             PRIMARY KEY (message_id)
);

-- 테이블 2: 사용자별 조회
CREATE TABLE IF NOT EXISTS chat_messages_by_user (
                                                     user_name TEXT,
                                                     created_at TIMESTAMP,
                                                     message_id UUID,
                                                     message TEXT,
                                                     kafka_partition INT,
                                                     kafka_offset BIGINT,
                                                     PRIMARY KEY ((user_name), created_at, message_id)
) WITH CLUSTERING ORDER BY (created_at DESC, message_id ASC);
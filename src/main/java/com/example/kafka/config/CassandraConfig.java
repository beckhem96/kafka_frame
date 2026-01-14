package com.example.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/**
 * Cassandra 설정 클래스
 *
 * Cassandra란?
 * - 분산 NoSQL 데이터베이스
 * - 높은 쓰기 성능 (초당 수만~수십만 건)
 * - 선형 확장성 (노드 추가로 성능 증가)
 * - 단일 장애 지점 없음 (모든 노드가 master)
 *
 * 데이터 모델:
 * - Keyspace: RDB의 Database와 유사
 * - Table: RDB의 Table과 유사
 * - Partition Key: 데이터를 어느 노드에 저장할지 결정
 * - Clustering Key: 파티션 내 정렬 기준
 *
 * 특징:
 * - 쓰기 성능이 매우 빠름 (메모리 → Commit Log → SSTable)
 * - 조인 불가 (비정규화 설계 필요)
 * - 집계 함수 제한적 (COUNT, SUM 등 느림)
 */

@Configuration
@EnableCassandraRepositories(basePackages = "com.example.kafka.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {
    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    /**
     * Keyspace 이름 반환
     * Keyspace는 RDB의 Database와 유사한 개념
     */
    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    /**
     * Cassandra 서버 주소
     */
    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    /**
     * Cassandra 포트
     */
    @Override
    protected int getPort() {
        return port;
    }

    /**
     * 로컬 데이터센터 이름
     * Cassandra는 여러 데이터센터에 분산 가능
     */
    @Override
    protected String getLocalDataCenter() {
        return localDatacenter;
    }

    /**
     * 스키마 액션
     * - NONE: 아무것도 안함
     * - CREATE: 없으면 생성
     * - CREATE_IF_NOT_EXISTS: 없으면 생성 (권장)
     * - RECREATE: 삭제 후 재생성 (개발 시에만!)
     * - RECREATE_DROP_UNUSED: 사용 안하는 것 삭제 후 재생성
     */
    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }
}

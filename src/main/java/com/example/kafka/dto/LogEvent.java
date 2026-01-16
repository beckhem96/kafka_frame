package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 로그 이벤트 DTO
 *
 * 역할:
 * - 구조화된 로그 데이터를 담는 객체
 * - JSON으로 직렬화되어 Kafka로 전송
 * - ELK에서 필드별로 검색 및 필터링 가능
 *
 * 사용 케이스:
 * - 비즈니스 이벤트 로깅
 * - 사용자 행동 추적
 * - 성능 메트릭 수집
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEvent {
    /**
     * 이벤트 타입
     *
     * 예:
     * - USER_LOGIN
     * - ORDER_CREATED
     * - PAYMENT_COMPLETED
     * - API_CALL
     */
    private String eventType;
    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 사용자 이름
     */
    private String userName;

    /**
     * 클라이언트 IP 주소
     */
    private String clientIp;

    /**
     * HTTP 메서드
     */
    private String httpMethod;

    /**
     * API 엔드포인트
     */
    private String endpoint;

    /**
     * 응답 시간 (밀리초)
     */
    private Long durationMs;

    /**
     * HTTP 상태 코드
     */
    private Integer statusCode;

    /**
     * 추가 데이터 (Key-Value)
     *
     * 유연하게 추가 정보를 담을 수 있음
     * 예:
     * - orderId: 123
     * - amount: 10000
     * - productName: "노트북"
     */
    private Map<String, Object> additionalData;

    /**
     * 이벤트 발생 시간
     *
     * @JsonFormat: JSON 직렬화 시 날짜 포맷 지정
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 애플리케이션 이름
     */
    private String application;

    /**
     * 환경 (dev, staging, prod)
     */
    private String environment;
}

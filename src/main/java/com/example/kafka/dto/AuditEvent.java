package com.example.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 감상 이벤트 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 수행 작업
     */
    private String action;

    /**
     * 대상 리소스
     */
    private String resource;

    /**
     * 리소스 ID
     */
    private String resourceId;

    /**
     * 결과 (SUCCESS, FAILURE)
     */
    private String result;

    /**
     * IP 주소
     */
    private String ipAddress;

    /**
     * User Agent
     */
    private String userAgent;

    /**
     * 추가 메타데이터
     */
    private Map<String, Object> metadata;

    /**
     * 이벤트 시간
     */
    private LocalDateTime timestamp;
}

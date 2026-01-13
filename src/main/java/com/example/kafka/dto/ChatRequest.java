package com.example.kafka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 요청 DTO
 *
 * 역할:
 * - Controller에서 JSON 요청을 받을 때 사용
 * - @Validated로 유효성 검증
 *
 * 예시 JSON:
 * {
 *   "user": "홍길동",
 *   "message": "안녕하세요"
 * }
 */
@Getter
@NoArgsConstructor
public class ChatRequest {

    /**
     * 사용자 이름
     * - 필수 입력
     * - 2-20자
     */
    @NotBlank(message = "사용자 이름은 필수입니다")
    @Size(min = 2, max = 20, message = "사용자 이름은 2-20자여야 합니다")
    private String user;

    /**
     * 메시지 내용
     * - 필수 입력
     * - 1-500자
     */
    @NotBlank(message = "메시지는 필수입니다")
    @Size(min = 1, max = 500, message = "메시지는 1-500자여야 합니다")
    private String message;
}
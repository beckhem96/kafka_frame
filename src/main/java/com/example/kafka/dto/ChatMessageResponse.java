package com.example.kafka.dto;

import com.example.kafka.entity.ChatMessageByUser;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 응답 DTO
 *
 * 역할:
 * - Entity → JSON 변환
 * - 클라이언트에게 반환할 데이터 구조
 */
@Getter
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private UUID messageId;
    private String userName;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환
     */
    public static ChatMessageResponse from(ChatMessageByUser entity) {
        return ChatMessageResponse.builder()
                .messageId(entity.getMessageId())
                .userName(entity.getUserName())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

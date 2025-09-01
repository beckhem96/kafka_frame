package com.example.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {
    @KafkaListener(topics = "${app.kafka.topic}", groupId = "chat-group")
    public void consume(String message) {
        // 필요한 동작 추가
        System.out.println("[Consumer] Received message: " + message);
    }
}

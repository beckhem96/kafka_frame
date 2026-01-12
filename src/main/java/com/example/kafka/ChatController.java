package com.example.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final KafkaProducerService producer;

    @PostMapping
    public String send(@RequestParam String user, @RequestBody String msg) {
        producer.send(user, msg);
        return "sent";
    }
}

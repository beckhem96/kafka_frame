package com.example.kafka;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private final KafkaProducerService producer;

    public ChatController(KafkaProducerService producer) {
        this.producer = producer;
    }

    @PostMapping
    public String send(@RequestParam String user, @RequestBody String msg) {
        producer.send(user, msg);
        return "sent";
    }
}

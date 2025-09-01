package com.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class ChatController {
//    private final KafkaProducerService producer;
private static final Logger log = LoggerFactory.getLogger(ChatController.class);

//    @PostMapping
//    public String send(@RequestParam String user, @RequestBody String msg) {
//        producer.send(user, msg);
//        return "sent";
//    }

    // 간단한 엔드포인트: 호출 시 로그가 Kafka로 전송되고, Logstash를 통해 ES로 색인됨
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "world") String name) {
        log.info("hello endpoint called name={}", name);
        return "hello " + name;
    }
}

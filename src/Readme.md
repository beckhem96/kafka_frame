## 프로젝트 구조 (최종)
```
src/main/java/com/example/kafka/
├── config/
│   ├── KafkaConfig.java              ⚙️ Kafka 설정
│   ├── KafkaProperties.java          📋 Kafka 설정 값
│   └── CassandraConfig.java          ⚙️ Cassandra 설정 (신규)
├── controller/
│   ├── ChatController.java           🌐 메시지 전송 API
│   └── ChatQueryController.java      🔍 메시지 조회 API (신규)
├── service/
│   ├── KafkaProducerService.java     📤 메시지 전송
│   ├── KafkaConsumerService.java     📥 메시지 수신 + Cassandra 저장 (수정)
│   └── ChatQueryService.java         🔍 메시지 조회 (신규)
├── entity/
│   ├── ChatMessage.java              💾 메시지 Entity (신규)
│   └── ChatMessageByUser.java        💾 사용자별 메시지 Entity (신규)
├── repository/
│   ├── ChatMessageRepository.java    📊 메시지 Repository (신규)
│   └── ChatMessageByUserRepository.java 📊 사용자별 Repository (신규)
├── dto/
│   ├── ChatRequest.java              📝 요청 DTO
│   └── ChatMessageResponse.java      📝 응답 DTO (신규)
└── KafkaApplication.java             🚀 메인 클래스
```
## 전체 동작 흐름 (Kafka + Cassandra)
```
[메시지 전송]
사용자: POST /api/chat {"user": "홍길동", "message": "안녕"}
   ↓
Controller: 요청 검증
   ↓
ProducerService: Kafka로 전송
   ↓
Kafka Broker: 메시지 저장 (임시)
   ↓
ConsumerService: 메시지 수신
   ↓
Cassandra: 2개 테이블에 저장 (영구)
   1. chat_messages (메시지 ID로 조회)
   2. chat_messages_by_user (사용자별 조회)
   ↓
오프셋 커밋
   ↓
저장 완료

[메시지 조회]
사용자: GET /api/chat/users/홍길동/messages/recent?limit=100
   ↓
QueryController: 요청 수신
   ↓
QueryService: Cassandra 조회
   ↓
Repository: CQL 실행
   SELECT * FROM chat_messages_by_user 
   WHERE user_name = '홍길동'
   ORDER BY created_at DESC
   LIMIT 100
   ↓
Entity → DTO 변환
   ↓
JSON 응답 반환
```


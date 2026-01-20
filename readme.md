# 대규모 프로젝트를 위한 하이브리드 방식 elk 프로젝트 구조

kafka-elk-enterprise/
├── docker/
│   ├── docker-compose.yml
│   └── logstash/
│       ├── config/
│       │   └── logstash.yml
│       └── pipeline/
│           └── logstash.conf
│
├── src/main/
│   ├── java/com/company/service/
│   │   ├── ServiceApplication.java
│   │   ├── config/
│   │   │   ├── LoggingConfig.java
│   │   │   └── KafkaConfig.java
│   │   ├── constants/
│   │   │   └── LogMarkers.java
│   │   ├── aspect/
│   │   │   ├── LoggingAspect.java
│   │   │   └── AuditAspect.java
│   │   ├── controller/
│   │   │   ├── OrderController.java
│   │   │   └── UserController.java
│   │   ├── service/
│   │   │   ├── OrderService.java
│   │   │   ├── UserService.java
│   │   │   └── logging/
│   │   │       ├── StructuredLoggingService.java
│   │   │       └── AuditLoggingService.java
│   │   └── dto/
│   │       └── AuditEvent.java
│   │
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       ├── logback-spring.xml
│       └── elk/
│           ├── index-templates/
│           │   ├── app-logs-template.json
│           │   ├── error-logs-template.json
│           │   └── audit-logs-template.json
│           └── ilm-policies/
│               └── logs-policy.json
│
└── pom.xml


application.yml (공통)
├─ spring.application.name
├─ service.version
└─ logging.config

application-dev.yml (개발)
├─ app.kafka.bootstrap-servers  ← KafkaProperties가 찾는 값
└─ kafka.bootstrap-servers       ← logback-spring.xml이 찾는 값

application-prod.yml (프로덕션)
├─ app.kafka.bootstrap-servers  ← KafkaProperties가 찾는 값
└─ kafka.bootstrap-servers       ← logback-spring.xml이 찾는 값
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



# 구조 분석 후 문제점 확인
---                                                                                                        
구조 분석 결과

잘 된 점

- 마커 기반 로그 라우팅으로 깔끔한 코드 유지
- MDC를 활용한 분산 추적 기반
- 환경별(dev/prod) 설정 분리
- 비동기 Appender로 성능 최적화
- 감사/보안 로그를 별도 토픽으로 분리

  ---                                                                                                        
문제점 및 개선 권장사항

1. 보안 이슈 (Critical)

문제: docker-compose.yml에서 Elasticsearch 보안 비활성화                                                   
xpack.security.enabled=false

위험:
- 개발 환경에서도 민감한 감사/보안 로그가 무보호 상태
- 네트워크 노출 시 데이터 유출 가능

개선: 최소한 기본 인증 활성화, 프로덕션에서는 TLS 필수
                                                                                                             
---                                                                                                        
2. 의존성 안정성 문제

문제: build.gradle:25                                                                                      
implementation 'com.github.danielwegener:logback-kafka-appender:0.2.0-RC2'

위험: RC(Release Candidate) 버전은 프로덕션에 부적합

개선: 안정 버전 사용 또는 Spring Kafka의 기본 메커니즘 활용
                                                                                                             
---                                                                                                        
3. Consumer 미구현

문제: Producer만 있고 Consumer가 없음

현재 구조:
- Controller → Service → Logger → Kafka → Logstash → ES

개선:
- 비즈니스 이벤트(ORDER_CREATED 등)를 처리하는 Consumer 추가
- 예: 재고 차감, 알림 발송 등

  ---                                                                                                        
4. 데이터 영속성 문제

문제: docker-compose.yml에 볼륨 설정 부재

elasticsearch:                                                                                             
- volumes: 없음                                                                                          
kafka:                                                                                                     
- volumes: 없음

위험: 컨테이너 재시작 시 모든 데이터 손실

개선:                                                                                                      
volumes:                                                                                                   
- es-data:/usr/share/elasticsearch/data                                                                  
- kafka-data:/var/lib/kafka/data
                                                                                                             
---                                                                                                        
5. Dead Letter Queue 미구현

문제: 로그 전송 실패 시 복구 불가

logback-spring.xml에서 실패 시 처리 로직 없음

개선:
- 로컬 파일 Fallback Appender 추가
- Kafka DLQ 토픽 설정

  ---                                                                                                        
6. Order 엔티티 미완성

문제: OrderService.java:42-46                                                                              
Order order = Order.builder()                                                                              
.id(orderId)                                                                                           
.amount(request.getAmount())                                                                           
.build();                                                                                              
// DB 저장 없음, 단순 로그만 기록

상태: 현재는 로깅 데모용으로만 작동
                                                                                                             
---                                                                                                        
7. Logstash 토픽 패턴 불일치 가능성

문제: logstash.conf와 logback-spring.xml 토픽명 검증 필요

- Logback: app-logs-${springAppName}
- Logstash: app-logs-.* (정규식)

서비스명에 특수문자 포함 시 매칭 실패 가능
                                                                                                             
---                                                                                                        
8. 메모리 설정 부족

문제: docker-compose.yml                                                                                   
ES_JAVA_OPTS: "-Xms512m -Xmx512m"                                                                          
LS_JAVA_OPTS: "-Xmx256m -Xms256m"

위험: 프로덕션 환경에서 OOM 가능성 높음

개선: 프로덕션용 compose 파일 분리, 적절한 리소스 할당
                                                                                                             
---                                                                                                        
9. 로그 필드 불일치

문제: logstash.conf:63-67                                                                                  
date {                                                                                                     
match => [ "timestamp", "ISO8601" ]                                                                      
}

Logback JSON 출력의 @timestamp 필드와 매칭 확인 필요
                                                                                                             
---                                                                                                        
10. 모니터링/알림 부재

문제:
- Kafka lag 모니터링 없음
- ES 클러스터 상태 알림 없음
- 에러 로그 급증 시 알림 없음

개선:
- Prometheus + Grafana 추가
- Elasticsearch Watcher 또는 ElastAlert 설정

  ---                                                                                                        
개선 우선순위                                                                                              
┌──────┬───────────────────────────┬──────────────────────┐                                                
│ 순위 │           항목            │        심각도        │                                                
├──────┼───────────────────────────┼──────────────────────┤                                                
│ 1    │ 데이터 영속성 (볼륨 설정) │ 높음                 │                                                
├──────┼───────────────────────────┼──────────────────────┤                                                
│ 2    │ 보안 설정                 │ 높음                 │                                                
├──────┼───────────────────────────┼──────────────────────┤                                                
│ 3    │ RC 의존성 교체            │ 중간                 │                                                
├──────┼───────────────────────────┼──────────────────────┤                                                
│ 4    │ DLQ 구현                  │ 중간                 │                                                
├──────┼───────────────────────────┼──────────────────────┤                                                
│ 5    │ 모니터링 추가             │ 중간                 │                                                
├──────┼───────────────────────────┼──────────────────────┤                                                
│ 6    │ Consumer 구현             │ 낮음 (데모 목적이면) │                                                
└──────┴───────────────────────────┴──────────────────────┘                                                
전체적으로 학습/데모 목적의 프레임워크로는 잘 구성되어 있습니다. 프로덕션 배포 전에는 위 항목들을 반드시   
보완해야 합니다.    
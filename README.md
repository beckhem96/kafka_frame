좋아, **Kafka ↔ ELK(Elasticsearch + Logstash + Kibana)** 를 한 번에 맛볼 수 있는 “가장 기본” 실습 예시를 드릴게요.
구성은 **Kafka(토픽) → Logstash(컨슈머) → Elasticsearch(색인) → Kibana(검색/시각화)** 입니다.

---

# 0) 폴더 구성

```
kafka-elk-lab/
 ├─ docker-compose.yml
 └─ logstash/
     └─ pipeline/
         └─ logstash.conf
```

---

# 1) docker-compose.yml

> 보안을 단순화하기 위해 **ELK 7.17.x** 버전을 사용합니다.

```yaml
version: "3.8"

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.1
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports: ["2181:2181"]

  kafka:
    image: confluentinc/cp-kafka:7.6.1
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.17.13
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  kibana:
    image: docker.elastic.co/kibana/kibana:7.17.13
    depends_on: [elasticsearch]
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
      - XPACK_SECURITY_ENABLED=false
    ports:
      - "5601:5601"

  logstash:
    image: docker.elastic.co/logstash/logstash:7.17.13
    depends_on: [kafka, elasticsearch]
    volumes:
      - ./logstash/pipeline/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro
    environment:
      LS_JAVA_OPTS: "-Xms256m -Xmx256m"
    ports:
      - "5044:5044"  # (옵션) Filebeat 같은 입력용 포트
```

---

# 2) Logstash 파이프라인 (Kafka → Elasticsearch)

`logstash/pipeline/logstash.conf` 파일 생성:

```conf
input {
  kafka {
    bootstrap_servers => "kafka:9092"
    topics            => ["app-logs"]
    group_id          => "logstash-consumer"
    auto_offset_reset => "latest"
    codec             => "json"
  }
}

filter {
  # 예: 타임스탬프 필드를 @timestamp로 매핑하거나, 필드 타입 변환 등
  # mutate { convert => { "latency_ms" => "integer" } }
}

output {
  elasticsearch {
    hosts  => ["http://elasticsearch:9200"]
    index  => "kafka-logs-%{+YYYY.MM.dd}"
    # ilm_enabled => false  # 7.x에서 간단히 쓰려면 비활성화 가능
  }
  stdout { codec => rubydebug } # 콘솔 디버깅용
}
```

---

# 3) 기동

```bash
docker compose up -d
# 서비스들이 모두 뜰 때까지 20~40초 정도 기다리세요 (ES/Kibana가 가장 오래 걸림)
```

---

# 4) 카프카 토픽 만들기

```bash
docker exec -it $(docker ps -qf "name=kafka") \
  kafka-topics --create --topic app-logs \
  --bootstrap-server kafka:9092 \
  --partitions 1 --replication-factor 1

# 확인
docker exec -it $(docker ps -qf "name=kafka") \
  kafka-topics --describe --topic app-logs --bootstrap-server kafka:9092
```

---

# 5) 메시지 보내보기 (Producer 콘솔)

아래 명령으로 **JSON 로그**를 전송합니다.

```bash
docker exec -it $(docker ps -qf "name=kafka") \
  kafka-console-producer --broker-list kafka:9092 --topic app-logs
```

터미널이 프로듀서 입력 모드로 바뀌면, 줄마다 한 건씩 붙여넣습니다(예시는 JSON):

```json
{"level":"INFO","service":"order-api","msg":"order created","orderId":101,"latency_ms":23,"ts":"2025-09-01T10:00:01Z"}
{"level":"WARN","service":"user-api","msg":"slow query","userId":42,"latency_ms":312,"ts":"2025-09-01T10:00:03Z"}
{"level":"ERROR","service":"payment-api","msg":"charge failed","code":"PMT-402","ts":"2025-09-01T10:00:05Z"}
```

> Logstash가 `codec => json`이므로 JSON 형태가 깔끔하게 파싱됩니다.

---

# 6) Kibana에서 확인

1. 브라우저로 `http://localhost:5601` 접속
2. 왼쪽 메뉴 **Discover** → 인덱스 패턴 생성

   * 패턴: `kafka-logs-*`
   * Time field: `@timestamp` (없으면 Logstash가 생성한 `@timestamp` 사용)
3. Discover에서 메시지들이 들어오는지 확인

   * 필터: `service: "order-api"`, `level: "ERROR"` 같은 쿼리
   * 필드(Columns)에 `service`, `msg`, `latency_ms` 추가해보기

---

# 7) (옵션) 애플리케이션에서 직접 Kafka로 로그 보내기

실무에선 앱 로거를 카프카로 보냅니다.

* **Logback + Kafka Appender**(Java/Spring)

  ```xml
  <!-- logback-spring.xml -->
  <configuration>
    <appender name="KAFKA" class="com.github.danielwegener.logback.kafka.KafkaAppender">
      <topic>app-logs</topic>
      <producerConfig>bootstrap.servers=localhost:9092</producerConfig>
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
      <appender-ref ref="KAFKA"/>
    </root>
  </configuration>
  ```

  > 의존성:
  > `ch.qos.logback:logback-classic`,
  > `net.logstash.logback:logstash-logback-encoder`,
  > `com.github.danielwegener:logback-kafka-appender`
  > (버전은 프로젝트 환경에 맞춰 주세요)

* 또는 **간단 Producer 코드**로 JSON 전송(앞서 보낸 콘솔 메시지와 동일 포맷).

---

# 8) 자주 막히는 포인트 (트러블슈팅)

* **Kibana 빈 화면**: ES가 아직 안 떠서 인덱스가 안 만들어졌을 수 있음 → 20\~40초 기다렸다가 새로고침
* **Logstash에서 메시지 못 읽음**: `topics`, `bootstrap_servers`, Logstash 로그 확인(`docker logs logstash`)
* **JSON 파싱 실패**: Producer에서 **정확한 JSON**을 보내야 함 (लाइन브레이크마다 한 건)
* **시간 필드**: JSON에 `ts`가 있다면 filter에서 `date { match => ["ts","ISO8601"] target => "@timestamp" }`로 매핑 가능

예: `logstash.conf` filter에 추가

```conf
filter {
  if [ts] {
    date {
      match  => ["ts", "ISO8601"]
      target => "@timestamp"
    }
    mutate { remove_field => ["ts"] }
  }
}
```

---

# 9) 확장 아이디어

* **파티션 확장**: `app-logs`를 3\~6 파티션으로 늘려 Logstash 멀티 작업(멀티 파이프라인/멀티 컨슈머)
* **인덱스 라이프사이클**: 7.x는 ILM 사용 가능 / 또는 날짜별 인덱스 롤오버
* **시각화**: Kibana에서 `level`별 파이 차트, `service`별 에러 트렌드 라인 차트
* **어노말리 탐지**: Kibana Machine Learning(유료/혹은 오픈 대체)

---

## 요약

* **Kafka → Logstash → Elasticsearch → Kibana** 파이프라인으로 **기본 로그 스트리밍**을 실습
* 콘솔 프로듀서로 JSON을 보내면, Kibana에서 바로 검색/시각화 가능
* 이후에는 **앱 로거 → Kafka**를 붙여 실전과 동일하게 확장하면 됩니다

원하시면 위 구성에 **Filebeat**를 추가해서 “애플리케이션 로그 파일 → Filebeat → Kafka → Logstash → ES” 흐름도 바로 확장해 드릴게요.


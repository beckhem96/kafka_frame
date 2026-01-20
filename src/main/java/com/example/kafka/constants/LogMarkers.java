package com.example.kafka.constants;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * 로그 마커 정의
 *
 * 마커를 사용하면 Logback이 자동으로 적절한 Kafka 토픽으로 라우팅합니다.
 *
 * 사용 예시:
 * log.info(LogMarkers.ACCESS, "User {} accessed resource", userId);
 * → access-logs-order-service 토픽으로 전송
 */
public class LogMarkers {
    /**
     * 접근 로그 마커 (HTTP 요청 등)
     * 토픽: access-logs-{service}
     */
    public static final Marker ACCESS = MarkerFactory.getMarker("ACCESS");

    /**
     * 감사 로그 마커 (중요 작업 추적)
     * 토픽: audit-logs (통합)
     */
    public static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");

    /**
     * 보안 로그 마커 (인증, 인가 실패 등)
     * 토픽: security-logs (통합)
     */
    public static final Marker SECURITY = MarkerFactory.getMarker("SECURITY");

    /**
     * 비즈니스 이벤트 마커 (주문 생성, 결제 완료 등)
     * 토픽: business-events-{service}
     */
    public static final Marker BUSINESS = MarkerFactory.getMarker("BUSINESS");

    /**
     * 메트릭 로그 마커 (성능 측정 등)
     * 토픽: metric-logs-{service}
     */
    public static final Marker METRIC = MarkerFactory.getMarker("METRIC");

    private LogMarkers() {
        // 인스턴스 생성 방지
    }
}

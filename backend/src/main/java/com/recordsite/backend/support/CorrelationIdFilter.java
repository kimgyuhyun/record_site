package com.recordsite.backend.support;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

// 요청마다 상관관계 ID 를 부여해 로그와 응답에 같은 값을 남긴다. 장애를 볼 때 "이 응답"과 "그때 찍힌 로그들"을
// 같은 키로 묶기 위한 것이다.
//
// 엣지 nginx 가 X-Request-Id 를 채워 보내면 그 값을 이어받아 엣지 로그와 앱 로그가 같은 키로 묶인다.
// 헤더가 없으면(로컬 개발) 여기서 만든다.
//
// MDC 에 넣은 값은 구조적 로깅(LOGGING_STRUCTURED_FORMAT_CONSOLE=logstash)이 JSON 필드로 자동 포함하므로
// 별도 로그 패턴 설정이 필요 없다. promtail 이 그 JSON 을 그대로 Loki 로 넘긴다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";
    private static final int MAX_LENGTH = 64;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestId = resolve((HttpServletRequest) request);
        MDC.put(MDC_KEY, requestId);
        ((HttpServletResponse) response).setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 톰캣이 스레드를 재사용하므로 반드시 지운다. 안 지우면 다음 요청 로그에 앞 요청의 ID 가 붙는다.
            MDC.remove(MDC_KEY);
        }
    }

    // 엣지를 거치면 nginx 가 값을 덮어쓰지만, 엣지를 안 거치는 경로도 있으므로 여기서도 형식을 제한한다.
    // 제한이 없으면 클라이언트가 개행이나 긴 문자열을 넣어 로그를 오염시킬 수 있다.
    private String resolve(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || header.isBlank()
                || header.length() > MAX_LENGTH
                || !header.matches("[A-Za-z0-9._-]+")) {
            return UUID.randomUUID().toString();
        }
        return header;
    }
}

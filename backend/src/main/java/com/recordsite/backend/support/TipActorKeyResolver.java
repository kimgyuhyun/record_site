package com.recordsite.backend.support;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

// 비로그인 게시판에서 "같은 사람"을 식별하는 키를 만든다. 계정이 없으므로 클라이언트 IP 를 쓰되,
// 원본 IP 는 저장하지 않고 솔트를 키로 한 HMAC-SHA256 결과만 남긴다
// (IPv4 는 경우의 수가 43억뿐이라, 솔트 없는 해시는 전수 대입으로 그대로 역산된다).
@Component
public class TipActorKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(TipActorKeyResolver.class);

    // 엣지 nginx 가 proxy_set_header 로 "덮어쓰는" 헤더. 클라이언트가 보낸 값은 무시되므로 신뢰할 수 있다.
    // ⚠️ X-Forwarded-For 를 쓰면 안 된다: nginx 가 $proxy_add_x_forwarded_for 로 클라이언트가 보낸 값
    //    "뒤에" 실제 IP 를 덧붙이는 방식이라, 헤더를 위조하면 앞쪽에 임의의 값을 무한히 만들어낼 수 있다.
    private static final String CLIENT_IP_HEADER = "X-Real-IP";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec saltKey;

    public TipActorKeyResolver(@Value("${tip.actor-salt:}") String configuredSalt) {
        String salt = configuredSalt;
        if (salt == null || salt.isBlank()) {
            // 운영 compose 는 이 값을 필수로 주입한다. 여기 걸리는 건 로컬 개발 환경이므로,
            // 빈 솔트로 IP 를 역산 가능하게 두느니 기동할 때마다 임의 솔트를 만든다
            // (재시작하면 키가 바뀌어 중복 방지가 초기화되지만, 개발 환경에서는 문제되지 않는다).
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            salt = HexFormat.of().formatHex(random);
            log.warn("tip.actor-salt 가 비어 있어 임시 솔트를 생성했습니다. 운영에서는 TIP_ACTOR_SALT 를 주입하세요.");
        }
        this.saltKey = new SecretKeySpec(salt.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String resolve(HttpServletRequest request) {
        return hash(clientIp(request));
    }

    // 엣지를 거치지 않는 로컬 개발에서는 헤더가 없으므로 소켓 주소로 폴백한다.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(CLIENT_IP_HEADER);
        return (forwarded == null || forwarded.isBlank()) ? request.getRemoteAddr() : forwarded.trim();
    }

    private String hash(String clientIp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(saltKey);
            return HexFormat.of().formatHex(mac.doFinal(clientIp.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("actor key 생성 실패", e);
        }
    }
}

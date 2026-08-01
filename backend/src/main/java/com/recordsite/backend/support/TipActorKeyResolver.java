package com.recordsite.backend.support;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    // 전달 헤더는 그 헤더를 채우는 프록시가 보냈을 때만 신뢰한다. 신뢰 판정 없이 읽으면 프록시를 우회해
    // 들어온 요청의 헤더까지 그대로 믿게 되고, 그러면 누구나 헤더를 바꿔가며 actor key 를 무한히 만들어
    // 1인 1회 제약을 무력화할 수 있다.
    //
    // 이 배포에서 엣지 nginx 는 항상 도커 내부망(사설 대역)에서 붙고 백엔드 포트는 호스트에 공개되지 않는다.
    // 그래서 "직전 홉이 사설/루프백 주소인가"로 프록시 여부를 판정한다. 공인 주소에서 직접 들어온 요청은
    // 엣지를 거치지 않았다는 뜻이므로 헤더를 버리고 소켓 주소를 쓴다.
    // 엣지를 거치지 않는 로컬 개발에서는 헤더 자체가 없어 그대로 소켓 주소로 폴백된다.
    private String clientIp(HttpServletRequest request) {
        if (!fromTrustedProxy(request)) {
            return request.getRemoteAddr();
        }
        String forwarded = request.getHeader(CLIENT_IP_HEADER);
        return (forwarded == null || forwarded.isBlank()) ? request.getRemoteAddr() : forwarded.trim();
    }

    private boolean fromTrustedProxy(HttpServletRequest request) {
        try {
            // IP 리터럴이라 DNS 조회가 일어나지 않는다.
            InetAddress remote = InetAddress.getByName(request.getRemoteAddr());
            return remote.isLoopbackAddress() || remote.isSiteLocalAddress() || remote.isLinkLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
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

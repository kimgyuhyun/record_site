package com.recordsite.backend.support;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

// 비로그인 팁 게시판의 삭제용 비밀번호를 해싱한다. 계정 비밀번호가 아니라 "내 글 삭제 키" 수준이지만,
// 평문 저장은 피하려고 PBKDF2(솔트 + 반복)로 저장한다. 외부 라이브러리 없이 JDK 만 사용한다.
// 저장 형식: base64(salt) + "$" + base64(hash)
@Component
public class TipPasswordEncoder {

    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String encode(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(raw, salt);
        return base64(salt) + "$" + base64(hash);
    }

    public boolean matches(String raw, String stored) {
        try {
            String[] parts = stored.split("\\$", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual = pbkdf2(raw, salt);
            return MessageDigest.isEqual(expected, actual); // 타이밍 공격 방지 비교
        } catch (RuntimeException e) {
            return false;
        }
    }

    private byte[] pbkdf2(String raw, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("비밀번호 해싱 실패", e);
        }
    }

    private String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}

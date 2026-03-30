package ai.local.nalbbun.infra.security.apikey.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API 키 AES-256-GCM 암복호화.
 * app.apikey.secret 을 32바이트(256bit) 패딩하여 키로 사용합니다.
 */
@Slf4j
@Component
public class ApiKeyCrypto {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LEN = 12;
    private static final int    GCM_TAG    = 128;

    private final SecretKey secretKey;

    public ApiKeyCrypto(
            @Value("${app.apikey.secret:nalbbun-api-key-secret-32bytes!!}") String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(raw, 0, key, 0, Math.min(raw.length, 32));
        this.secretKey = new SecretKeySpec(key, "AES");
    }

    /** 평문 → Base64(IV + 암호화된 데이터) */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[GCM_IV_LEN + enc.length];
            System.arraycopy(iv,  0, out, 0,         GCM_IV_LEN);
            System.arraycopy(enc, 0, out, GCM_IV_LEN, enc.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("API 키 암호화 실패", e);
        }
    }

    /** Base64(IV + 암호화된 데이터) → 평문 */
    public String decrypt(String encoded) {
        try {
            byte[] raw = Base64.getDecoder().decode(encoded);
            byte[] iv  = new byte[GCM_IV_LEN];
            System.arraycopy(raw, 0, iv, 0, GCM_IV_LEN);
            byte[] enc = new byte[raw.length - GCM_IV_LEN];
            System.arraycopy(raw, GCM_IV_LEN, enc, 0, enc.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG, iv));
            return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("API 키 복호화 실패 (키 손상 또는 secret 불일치): {}", e.getMessage());
            return "";
        }
    }
}

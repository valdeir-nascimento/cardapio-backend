package com.cardapio.ordering.infrastructure.qr;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class QrSignedUrl {

    private QrSignedUrl() {}

    public static String sign(String path, long expEpochSeconds, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((path + "|" + expEpochSeconds).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign QR url", e);
        }
    }

    public static boolean verify(String path, long expEpochSeconds, String providedSig, String secret) {
        if (providedSig == null) return false;
        String expected = sign(path, expEpochSeconds, secret);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = providedSig.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}

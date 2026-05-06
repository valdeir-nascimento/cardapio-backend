package com.cardapio.payment.infrastructure.mercadopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Verifies MP webhook HMAC-SHA256 signature.
 *
 * MP sends headers:
 *   x-signature: ts=<timestamp>,v1=<hex-hmac>
 *   x-request-id: <id>
 *
 * Signed manifest format: id:<dataId>;request-id:<requestId>;ts:<ts>;
 * The dataId is read from the JSON body at $.data.id.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoSignatureVerifier {

    private final MercadoPagoProperties props;
    private final ObjectMapper json = new ObjectMapper();

    public boolean verify(String rawBody, Map<String, String> headers) {
        String signatureHeader = headerCi(headers, "x-signature");
        String requestId = headerCi(headers, "x-request-id");
        if (signatureHeader == null || requestId == null) {
            log.debug("missing x-signature/x-request-id headers");
            return false;
        }

        Map<String, String> parts = parseSignature(signatureHeader);
        String ts = parts.get("ts");
        String v1 = parts.get("v1");
        if (ts == null || v1 == null) {
            log.debug("malformed x-signature header");
            return false;
        }

        String dataId = extractDataId(rawBody);
        if (dataId == null) {
            log.debug("could not extract data.id from webhook body");
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String expected = hmacSha256Hex(props.webhookSecret(), manifest);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            v1.getBytes(StandardCharsets.UTF_8));
    }

    private static String headerCi(Map<String, String> headers, String name) {
        for (var e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    private static Map<String, String> parseSignature(String header) {
        Map<String, String> result = new HashMap<>();
        for (String segment : header.split(",")) {
            int idx = segment.indexOf('=');
            if (idx > 0) {
                result.put(segment.substring(0, idx).trim(), segment.substring(idx + 1).trim());
            }
        }
        return result;
    }

    private String extractDataId(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode root = json.readTree(body);
            JsonNode data = root.get("data");
            if (data == null) return null;
            JsonNode id = data.get("id");
            return id != null ? id.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String hmacSha256Hex(String secret, String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }
}

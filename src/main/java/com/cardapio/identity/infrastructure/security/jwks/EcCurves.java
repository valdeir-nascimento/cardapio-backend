package com.cardapio.identity.infrastructure.security.jwks;

import java.security.AlgorithmParameters;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

/**
 * Resolves named EC curves (e.g. P-256 for ES256 used by Apple) to the
 * java.security.spec parameter set. Only the curves we actually need for
 * social ID token verification are supported.
 */
final class EcCurves {

    private EcCurves() {}

    static ECParameterSpec params(String curve) {
        String stdName = switch (curve) {
            case "P-256" -> "secp256r1";
            case "P-384" -> "secp384r1";
            case "P-521" -> "secp521r1";
            default -> throw new IllegalArgumentException("unsupported EC curve: " + curve);
        };
        try {
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec(stdName));
            return params.getParameterSpec(ECParameterSpec.class);
        } catch (Exception e) {
            throw new IllegalStateException("cannot resolve EC params for " + curve, e);
        }
    }
}

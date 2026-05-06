package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.TableQrView;

import java.net.URI;
import java.time.Instant;

public record TableQrResponse(URI presignedUrl, Instant expiresAt) {
    public static TableQrResponse from(TableQrView v) {
        return new TableQrResponse(v.presignedUrl(), v.expiresAt());
    }
}

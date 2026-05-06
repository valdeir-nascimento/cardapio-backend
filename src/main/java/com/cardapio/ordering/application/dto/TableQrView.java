package com.cardapio.ordering.application.dto;

import java.net.URI;
import java.time.Instant;

public record TableQrView(URI presignedUrl, Instant expiresAt) {}

package com.cardapio.ordering.api;

import com.cardapio.ordering.infrastructure.qr.FilesystemQrProperties;
import com.cardapio.ordering.infrastructure.qr.FilesystemQrStorage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import static com.cardapio.ordering.infrastructure.qr.QrSignedUrl.verify;

@RestController
@RequestMapping("/api/v1/qr")
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
class QrImageController {

    private final FilesystemQrStorage storage;
    private final FilesystemQrProperties props;
    private final Clock clock;

    @GetMapping("/**")
    ResponseEntity<byte[]> serve(
        HttpServletRequest req,
        @RequestParam long exp,
        @RequestParam String sig
    ) throws IOException {
        String path = req.getRequestURI();
        if (clock.instant().getEpochSecond() > exp) {
            throw new ResponseStatusException(HttpStatus.GONE, "url expired");
        }
        if (!verify(path, exp, sig, props.signingSecret())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid signature");
        }
        String prefix = "/api/v1/qr/";
        if (!path.startsWith(prefix)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String key = path.substring(prefix.length());
        Path file = storage.resolveSafe(key);
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        byte[] bytes = Files.readAllBytes(file);
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(bytes);
    }
}

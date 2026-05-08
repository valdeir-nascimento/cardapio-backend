package com.cardapio.ordering.infrastructure.qr;

import com.cardapio.ordering.domain.port.QrStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;

/**
 * Free, self-hosted alternative to R2/S3. Persists QR PNGs under a local
 * directory and exposes them via a signed, time-limited URL served by
 * {@link QrImageController}.
 */
@Component
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "false", matchIfMissing = true)
@EnableConfigurationProperties(FilesystemQrProperties.class)
@Slf4j
public class FilesystemQrStorage implements QrStorage {

    private final FilesystemQrProperties props;
    private final Clock clock;
    private final Path root;

    public FilesystemQrStorage(FilesystemQrProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
        this.root = Path.of(props.path()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create QR storage dir at " + root, e);
        }
        log.info("Filesystem QR storage initialized at {}", root);
    }

    @Override
    public void putIfAbsent(String key, byte[] bytes, String contentType) {
        Path target = resolveSafe(key);
        if (Files.exists(target)) return;
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write QR image " + key, e);
        }
    }

    @Override
    public URI presignedUrl(String key, Duration ttl) {
        long exp = clock.instant().plus(ttl).getEpochSecond();
        String path = "/api/v1/qr/" + key;
        String sig = QrSignedUrl.sign(path, exp, props.signingSecret());
        String base = props.publicBaseUrl().replaceAll("/+$", "");
        return URI.create(base + path + "?exp=" + exp + "&sig=" + sig);
    }

    /** Resolves {@code key} under the storage root, rejecting traversal attempts. */
    public Path resolveSafe(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid QR key: " + key);
        }
        return resolved;
    }

    Path root() { return root; }
}

package com.cardapio.ordering.api;

import com.cardapio.ordering.infrastructure.qr.FilesystemQrProperties;
import com.cardapio.ordering.infrastructure.qr.FilesystemQrStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ordering — QR Image",
    description = "Serve as imagens PNG dos QR Codes das mesas via URL assinada com TTL.")
@SecurityRequirements
class QrImageController {

    private final FilesystemQrStorage storage;
    private final FilesystemQrProperties props;
    private final Clock clock;

    @Operation(summary = "Serve a imagem PNG do QR Code da mesa",
        description = """
            Endpoint público acessado pela URL gerada por
            `GET /api/v1/admin/tables/{id}/qr` (campo `presignedUrl`).
            Não chame diretamente — sempre passe pelo gerador de URL para ter
            os parâmetros `exp` (expiração) e `sig` (HMAC-SHA256).
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagem PNG retornada.",
            content = @Content(mediaType = "image/png",
                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "403", description = "Assinatura inválida.",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Arquivo não encontrado.",
            content = @Content),
        @ApiResponse(responseCode = "410", description = "URL expirou — gere uma nova.",
            content = @Content)
    })
    @GetMapping("/**")
    ResponseEntity<byte[]> serve(
        @Parameter(hidden = true) HttpServletRequest req,
        @Parameter(description = "Timestamp UNIX (segundos) de expiração da URL.", example = "1762812800")
        @RequestParam long exp,
        @Parameter(description = "Assinatura HMAC-SHA256 base64-url do path + exp.", example = "abc123def456")
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

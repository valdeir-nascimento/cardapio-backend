package com.cardapio.api.error;

import com.cardapio.shared.domain.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        log.info("domain rule violated: code={} message={}", ex.code(), ex.getMessage());
        ProblemDetail pd = ProblemDetails.fromDomainException(ex);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        log.info("not found: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(404);
        return ResponseEntity.status(404).body(pd);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex) {
        log.info("unauthorized: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(401);
        return ResponseEntity.status(401).body(pd);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ProblemDetail> handleNotification(NotificationException ex) {
        log.info("notification errors: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("unexpected error", ex);
        return ResponseEntity.internalServerError().body(ProblemDetails.unexpected());
    }
}

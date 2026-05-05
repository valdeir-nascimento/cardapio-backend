package com.cardapio.api.error;

import com.cardapio.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        log.info("domain rule violated: code={} message={}", ex.code(), ex.getMessage());
        ProblemDetail pd = ProblemDetails.fromDomainException(ex);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ProblemDetail> handleNotification(NotificationException ex) {
        log.info("notification errors: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex; // let Spring Security translate to 403
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("unexpected error", ex);
        return ResponseEntity.internalServerError().body(ProblemDetails.unexpected());
    }
}

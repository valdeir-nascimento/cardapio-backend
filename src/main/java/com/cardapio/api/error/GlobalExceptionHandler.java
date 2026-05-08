package com.cardapio.api.error;

import com.cardapio.shared.domain.DomainException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
            .<Map<String, Object>>map(fe -> Map.of(
                "field", fe.getField(),
                "code", fe.getCode() == null ? "INVALID" : fe.getCode().toUpperCase(),
                "message", fe.getDefaultMessage() == null ? "valor inválido" : fe.getDefaultMessage()))
            .toList();
        log.info("bean validation failed: {}", errors);
        return ResponseEntity.badRequest().body(ProblemDetails.validation(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errors = ex.getConstraintViolations().stream()
            .<Map<String, Object>>map(v -> Map.of(
                "field", v.getPropertyPath().toString(),
                "code", v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().toUpperCase(),
                "message", v.getMessage()))
            .toList();
        log.info("constraint violation: {}", errors);
        return ResponseEntity.badRequest().body(ProblemDetails.validation(errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> error = Map.of(
            "field", ex.getName(),
            "code", "TYPE_MISMATCH",
            "message", "valor inválido para o parâmetro");
        log.info("type mismatch on {}: {}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(ProblemDetails.validation(List.of(error)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        log.info("body not readable: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Corpo da requisição inválido");
        pd.setDetail("JSON malformado ou ausente");
        return ResponseEntity.badRequest().body(pd);
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

package com.nti.nti_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Наші бізнес-помилки (RuntimeException)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(
            RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error(e.getMessage()));
    }

    // Spring Security: акаунт вимкнений (enabled=false)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabled(
            DisabledException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error("Акаунт очікує схвалення адміністратора"));
    }

    // Spring Security: невірний пароль
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error("Невірний email або пароль"));
    }

    // Spring Security: акаунт заблокований
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(
            LockedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error("Акаунт заблоковано. Зверніться до адміністратора."));
    }

    // Валідація полів (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Невірні дані запиту");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error(message));
    }

    private Map<String, String> error(String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", message);
        return body;
    }
}
package com.dku.voice.voice_backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 요청 파라미터 (400)
     * - 존재하지 않는 메뉴 ID
     * - 결제 금액 위변조
     * - 지원하지 않는 결제 수단
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, e.getMessage()));
    }

    /**
     * 충돌 상태 (409)
     * - 이미 결제 완료된 주문
     * - 중복 paymentKey
     * - 취소 불가능한 결제 상태
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(409)
                .body(ErrorResponse.of(409, e.getMessage()));
    }

    /**
     * @Valid 검증 실패 (400)
     * - 필수 필드 누락
     * - 수량 1 미만
     * - 금액 0 이하
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, message));
    }

    /**
     * 예상치 못한 서버 오류 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(500, "서버 오류가 발생했습니다."));
    }

    // ── ErrorResponse ────────────────────────────────────────────────────────

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;

        public static ErrorResponse of(int status, String message) {
            return new ErrorResponse(status, message, LocalDateTime.now());
        }
    }
}
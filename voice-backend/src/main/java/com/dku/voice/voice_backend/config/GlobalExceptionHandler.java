package com.dku.voice.voice_backend.config;

import com.dku.voice.voice_backend.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 요청 파라미터 (400)
     * - 존재하지 않는 메뉴 ID
     * - 결제 금액 위변조
     * - 지원하지 않는 결제 수단
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[400] {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 충돌 상태 (409)
     * - 이미 결제 완료된 주문
     * - 중복 pgTransactionId
     * - 취소 불가능한 결제 상태
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        log.warn("[409] {}", e.getMessage());
        return ResponseEntity.status(409)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * @Valid 검증 실패 (400)
     * - 필수 필드 누락
     * - 수량 1 미만
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[400] validation 실패: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message));
    }

    /**
     * 예상치 못한 서버 오류 (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[500] {}", e.getMessage(), e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다."));
    }
}
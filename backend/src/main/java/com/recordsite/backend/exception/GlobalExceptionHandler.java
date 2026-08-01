package com.recordsite.backend.exception;

import com.recordsite.backend.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

// 예외를 HTTP 응답으로 바꾸는 유일한 지점이다. Controller 와 Service 는 상태 코드를 알지 않는다.
// 응답에는 에러 코드와 사용자용 메시지만 담는다 — 원본 예외 메시지·스택 트레이스는 로그에만 남긴다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 조회 대상 없음. 이 두 예외의 메시지는 내부 표기("summoner not found: ...")라 응답에 쓰지 않고
    // 고정 문구를 내려보낸다. 검색어가 섞인 원본은 로그로만 남긴다.
    @ExceptionHandler({SummonerNotFoundException.class, ChampionNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        log.debug("조회 대상 없음: {}", ex.getMessage());
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 대상을 찾을 수 없습니다.");
    }

    // 아래 팁 예외들의 메시지는 처음부터 사용자에게 보여주려고 쓴 문구라 그대로 내려보낸다.
    @ExceptionHandler(TipNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTipNotFound(TipNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "TIP_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(TipPasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(TipPasswordMismatchException ex) {
        return response(HttpStatus.FORBIDDEN, "TIP_PASSWORD_MISMATCH", ex.getMessage());
    }

    @ExceptionHandler(InvalidTipRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTipRequest(InvalidTipRequestException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(DuplicateTipInteractionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateInteraction(DuplicateTipInteractionException ex) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_INTERACTION", ex.getMessage());
    }

    // @Valid 실패. 위반이 여러 건이어도 사용자에게는 첫 건만 보여준다(요청 DTO 에 한국어 문구를 적어둔다).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}

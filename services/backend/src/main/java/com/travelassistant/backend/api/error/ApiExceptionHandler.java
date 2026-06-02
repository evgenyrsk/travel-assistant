package com.travelassistant.backend.api.error;

import com.travelassistant.backend.api.model.ApiModels.ErrorResponse;
import com.travelassistant.backend.api.model.ApiModels.ValidationErrorResponse;
import com.travelassistant.backend.api.model.ApiModels.ValidationFieldError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ValidationFieldError> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toValidationFieldError)
                .toList();
        var response = new ValidationErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                "Request validation failed.",
                requestId(request),
                fields
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        var response = new ErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                requestId(request),
                Map.of()
        );
        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGenericException(Exception exception, HttpServletRequest request) {
        var response = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR,
                "Internal server error.",
                requestId(request),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ValidationFieldError toValidationFieldError(FieldError error) {
        return new ValidationFieldError(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }

    private String requestId(HttpServletRequest request) {
        var header = request.getHeader("X-Request-Id");
        return header == null || header.isBlank() ? null : header;
    }
}


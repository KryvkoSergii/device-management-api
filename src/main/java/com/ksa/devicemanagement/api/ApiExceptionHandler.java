package com.ksa.devicemanagement.api;

import com.ksa.devicemanagement.generated.model.Error;
import com.ksa.devicemanagement.exception.DeviceInUseException;
import com.ksa.devicemanagement.exception.DeviceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(DeviceNotFoundException.class)
    ResponseEntity<Error> notFound(HttpServletRequest request, DeviceNotFoundException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildError(exception.getCode(), "Device not found", exception.getMessage(), request));
    }

    @ExceptionHandler(DeviceInUseException.class)
    ResponseEntity<Error> inUse(HttpServletRequest request, DeviceInUseException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(exception.getCode(), "Device in use", exception.getMessage(), request));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Error> conflict(HttpServletRequest request, Exception exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError("CONFLICT", "Device conflict",
                        "The device was modified by another request", request));
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<Error> badRequest(HttpServletRequest request, Exception exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError("BAD_REQUEST", "Invalid request", exception.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Error> typeMismatch(HttpServletRequest request, MethodArgumentTypeMismatchException exception) {
        String parameter = exception.getName();
        Error error = buildError(
                "BAD_REQUEST",
                "Invalid request parameter",
                "Invalid value for query parameter '" + parameter + "'",
                request);
        error.setErrors(List.of(parameter + ": unsupported value '" + exception.getValue() + "'"));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Error> validation(HttpServletRequest request, MethodArgumentNotValidException exception) {
        Error detail = buildError("BAD_REQUEST", "Validation failed", "Request contains invalid fields", request);
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList();
        detail.setErrors(errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(detail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Error> internalServerError(HttpServletRequest request, Exception exception) {
        log.error("Internal server error", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError("INTERNAL_SERVER_ERROR", "Internal server error",
                        "An unexpected error occurred", request));
    }

    private Error buildError(String code, String title, String message, HttpServletRequest request) {
        Error error = new Error();
        error.setTitle(title);
        error.setDetail(message);
        error.setCode(code);
        error.setInstance(URI.create(request.getRequestURI()));
        return error;
    }
}

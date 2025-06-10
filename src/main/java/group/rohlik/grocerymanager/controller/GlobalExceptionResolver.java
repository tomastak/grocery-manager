package group.rohlik.grocerymanager.controller;

import group.rohlik.grocerymanager.dto.ErrorTO;
import group.rohlik.grocerymanager.exception.*;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionResolver extends DefaultHandlerExceptionResolver {

    private static final int MAX_MESSAGE_DETAIL_LENGTH = 2000;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorTO messageNotReadableException(final HttpMessageNotReadableException ex) {
        String firstLine = null;
        var message = Optional.ofNullable(ex.getMessage());
        if (message.isPresent()) {
            firstLine = message.get().split("\n", 2)[0];
            if (firstLine.length() > MAX_MESSAGE_DETAIL_LENGTH) {
                firstLine = firstLine.substring(0, MAX_MESSAGE_DETAIL_LENGTH);
            }
        }
        return ErrorTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(Optional.ofNullable(firstLine).orElse(HttpStatus.BAD_REQUEST.name()))
                .message("Invalid request body")
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorTO handleValidationExceptions(MethodArgumentNotValidException ex) {
        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            var fieldName = ((FieldError) error).getField();
            var errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ErrorTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation failed")
                .message("Invalid input parameters")
                .data(errors)
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorTO handleConstraintViolationException(ConstraintViolationException ex) {
        final Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            var propertyPath = violation.getPropertyPath().toString();
            var errorMessage = violation.getMessage();
            errors.put(propertyPath, errorMessage);
        });
        return ErrorTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation failed")
                .message("Invalid input parameters")
                .data(errors)
                .build();
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ErrorTO handleConcurrencyFailureException(ConcurrencyFailureException ex) {
        log.error("Concurrency failure: {}", ex.getMessage(), ex);
        return ErrorTO.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Concurrent Modification")
                .message("The resource was modified by another user. Please refresh and try again.")
                .build();
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ErrorTO handleProductAlreadyExistsException(ProductAlreadyExistsException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Product already exists")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ProductDeletionException.class)
    public ErrorTO handleProductDeletionException(ProductDeletionException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Product deletion error")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProductNotFoundException.class)
    public ErrorTO handleProductNotFoundException(ProductNotFoundException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Product not found")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InsufficientStockException.class)
    public ErrorTO handleInsufficientStockException(InsufficientStockException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Insufficient stock")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidOrderStatusException.class)
    public ErrorTO handleInvalidOrderStatusException(InvalidOrderStatusException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid order status")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(OrderExpiredException.class)
    public ErrorTO handleOrderExpiredException(OrderExpiredException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Order expired")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(OrderNotFoundException.class)
    public ErrorTO handleOrderNotFoundException(OrderNotFoundException ex) {
        return ErrorTO.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Order not found")
                .message(ex.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorTO handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ErrorTO.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();
    }
}

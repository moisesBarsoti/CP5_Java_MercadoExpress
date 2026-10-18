
package br.com.fiap.mercadoexpress.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception
    ) {

        String message = exception.getMessage();

        HttpStatus status = switch (message) {
            case "Product not found",
                 "Order not found",
                 "User not found" -> HttpStatus.NOT_FOUND;

            case "Email already registered" -> HttpStatus.CONFLICT;

            default -> HttpStatus.BAD_REQUEST;
        };

        return buildResponse(status, message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(
            IllegalStateException exception
    ) {

        if ("Insufficient stock".equals(exception.getMessage())) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "Insufficient stock"
            );
        }

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error"
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(
            BadCredentialsException exception
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception
    ) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField() + ": " +
                                error.getDefaultMessage()
                )
                .orElse("Invalid request data");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request body"
        );
    }

    private ResponseEntity<ProblemDetail> buildResponse(
            HttpStatus status,
            String message
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                message
        );

        problem.setTitle(status.getReasonPhrase());

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}

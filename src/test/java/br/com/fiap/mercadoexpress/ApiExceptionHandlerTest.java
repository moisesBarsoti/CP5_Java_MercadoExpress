
package br.com.fiap.mercadoexpress;

import br.com.fiap.mercadoexpress.exception.ApiExceptionHandler;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler =
            new ApiExceptionHandler();

    @Test
    void shouldReturnNotFoundWhenProductDoesNotExist() {

        ResponseEntity<ProblemDetail> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException(
                                "Product not found"
                        )
                );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());

        assertEquals(
                "Product not found",
                response.getBody().getDetail()
        );
    }

    @Test
    void shouldReturnBadRequestForInvalidOrder() {

        ResponseEntity<ProblemDetail> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException(
                                "Invalid order item"
                        )
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictWhenStockIsInsufficient() {

        ResponseEntity<ProblemDetail> response =
                handler.handleIllegalState(
                        new IllegalStateException(
                                "Insufficient stock"
                        )
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() {

        ResponseEntity<ProblemDetail> response =
                handler.handleIllegalArgument(
                        new IllegalArgumentException(
                                "Email already registered"
                        )
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() {

        ResponseEntity<ProblemDetail> response =
                handler.handleBadCredentials(
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldHideUnexpectedInternalErrors() {

        ResponseEntity<ProblemDetail> response =
                handler.handleIllegalState(
                        new IllegalStateException(
                                "Internal processing failure"
                        )
                );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "Unexpected server error",
                response.getBody().getDetail()
        );
    }
}
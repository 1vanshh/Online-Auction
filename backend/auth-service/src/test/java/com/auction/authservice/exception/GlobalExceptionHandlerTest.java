package com.auction.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleValidationException() throws Exception {
        SampleRequest payload = new SampleRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(payload, "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", "name", "must not be blank"));
        bindingResult.addError(new FieldError("sampleRequest", "email", "must be a well-formed email address"));

        Method method = DummyController.class.getDeclaredMethod("create", SampleRequest.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(new HandlerMethod(new DummyController(), method).getMethodParameters()[0], bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/dummy");

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals("/dummy", response.getBody().getPath());
        assertEquals("must not be blank", response.getBody().getFieldErrors().get("name"));
        assertEquals("must be a well-formed email address", response.getBody().getFieldErrors().get("email"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldHandleBadRequestException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/auth/register");

        ResponseEntity<ApiError> response = handler.handleBadRequest(new BadRequestException("Email already in use"), request);

        assertCommonError(response, 400, "Bad Request", "Email already in use", "/auth/register");
    }

    @Test
    void shouldHandleUnauthorizedException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/users/me");

        ResponseEntity<ApiError> response = handler.handleUnauthorized(new UnauthorizedException("Unauthorized"), request);

        assertCommonError(response, 401, "Unauthorized", "Unauthorized", "/users/me");
    }

    @Test
    void shouldHandleNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/users/admin/99");

        ResponseEntity<ApiError> response = handler.handleNotFound(new NotFoundException("User not found"), request);

        assertCommonError(response, 404, "Not Found", "User not found", "/users/admin/99");
    }

    @Test
    void shouldHandleGenericException() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/boom");

        ResponseEntity<ApiError> response = handler.handleGeneric(new IllegalStateException("boom"), request);

        assertCommonError(response, 500, "Internal Server Error", "Unexpected error", "/boom");
    }

    private static void assertCommonError(ResponseEntity<ApiError> response,
                                          int status,
                                          String error,
                                          String message,
                                          String path) {
        assertEquals(status, response.getBody().getStatus());
        assertEquals(error, response.getBody().getError());
        assertEquals(message, response.getBody().getMessage());
        assertEquals(path, response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @RestController
    static class DummyController {
        @PostMapping
        void create(@Valid @RequestBody SampleRequest request) {
        }
    }

    static class SampleRequest {
        @NotBlank
        private String name;
    }
}

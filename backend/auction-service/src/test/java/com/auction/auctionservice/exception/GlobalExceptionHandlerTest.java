package com.auction.auctionservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleValidationException() throws Exception {
        SampleRequest payload = new SampleRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(payload, "sampleRequest");
        bindingResult.addError(new FieldError("sampleRequest", "title", "must not be blank"));
        bindingResult.addError(new FieldError("sampleRequest", "categoryId", "must not be null"));

        Method method = DummyController.class.getDeclaredMethod("create", SampleRequest.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new HandlerMethod(new DummyController(), method).getMethodParameters()[0],
                bindingResult
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/lots");

        ResponseEntity<ApiError> response = handler.validation(ex, request);

        assertCommonError(response, 400, "Bad Request", "Validation failed", "/lots");
        assertEquals("must not be blank", response.getBody().getFieldErrors().get("title"));
        assertEquals("must not be null", response.getBody().getFieldErrors().get("categoryId"));
    }

    @Test
    void shouldHandleBadRequestException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/lots");

        ResponseEntity<ApiError> response = handler.bad(new BadRequestException("Bad lot request"), request);

        assertCommonError(response, 400, "Bad Request", "Bad lot request", "/lots");
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/lots/1/ban-unpaid-winner");

        ResponseEntity<ApiError> response = handler.illegalArgument(new IllegalArgumentException("banDays must be positive"), request);

        assertCommonError(response, 400, "Bad Request", "banDays must be positive", "/admin/lots/1/ban-unpaid-winner");
    }

    @Test
    void shouldHandleNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/lots/99");

        ResponseEntity<ApiError> response = handler.notFound(new NotFoundException("Lot not found"), request);

        assertCommonError(response, 404, "Not Found", "Lot not found", "/lots/99");
    }

    @Test
    void shouldHandleForbiddenException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/lots/1");

        ResponseEntity<ApiError> response = handler.forbidden(new ForbiddenException("Forbidden"), request);

        assertCommonError(response, 403, "Forbidden", "Forbidden", "/lots/1");
    }

    @Test
    void shouldHandleGenericException() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/boom");

        ResponseEntity<ApiError> response = handler.general(new RuntimeException("boom"), request);

        assertCommonError(response, 500, "Internal Server Error", "Unexpected error", "/boom");
    }

    private static void assertCommonError(ResponseEntity<ApiError> response,
                                          int status,
                                          String error,
                                          String message,
                                          String path) {
        assertEquals(status, response.getStatusCode().value());
        assertNotNull(response.getBody());
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
        private String title;
    }
}

package com.auction.biddingservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/test");
    }

    @Test
    void shouldHandleBadRequestException() {
        ResponseEntity<ApiError> response = handler.bad(new BadRequestException("Bad data"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Bad data");
        assertThat(response.getBody().getPath()).isEqualTo("/test");
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        ResponseEntity<ApiError> response = handler.illegalArgument(new IllegalArgumentException("Invalid argument"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");
    }

    @Test
    void shouldHandleNotFoundException() {
        ResponseEntity<ApiError> response = handler.notFound(new NotFoundException("Missing"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Missing");
    }

    @Test
    void shouldHandleForbiddenException() {
        ResponseEntity<ApiError> response = handler.forbidden(new ForbiddenException("Forbidden"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Forbidden");
    }

    @Test
    void shouldHandleGeneralException() {
        ResponseEntity<ApiError> response = handler.general(new RuntimeException("Boom"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error");
    }
}

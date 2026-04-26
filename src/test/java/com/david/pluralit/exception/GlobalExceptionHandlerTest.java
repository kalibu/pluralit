package com.david.pluralit.exception;

import com.david.pluralit.model.enums.ProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleInvalidProcessState_shouldReturnBadRequest() {
        UUID processId = UUID.randomUUID();
        InvalidProcessStateException exception = new InvalidProcessStateException(
                processId,
                ProcessStatus.COMPLETED,
                "stop"
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                globalExceptionHandler.handleInvalidProcessState(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isEqualTo(processId);
        assertThat(response.getBody().getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getBody().getMessage()).isNotNull();
    }

    @Test
    void handleInvalidProcessState_shouldIncludeErrorMessage() {
        UUID processId = UUID.randomUUID();
        InvalidProcessStateException exception = new InvalidProcessStateException(
                processId,
                ProcessStatus.FAILED,
                "pause"
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                globalExceptionHandler.handleInvalidProcessState(exception);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("FAILED");
    }

    @Test
    void handleInvalidProcessState_shouldReturnValidStructure() {
        UUID processId = UUID.randomUUID();
        InvalidProcessStateException exception = new InvalidProcessStateException(
                processId,
                ProcessStatus.STOPPED,
                "resume"
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                globalExceptionHandler.handleInvalidProcessState(exception);

        GlobalExceptionHandler.ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getProcessId()).isNotNull();
        assertThat(body.getStatus()).isNotNull();
        assertThat(body.getMessage()).isNotNull();
    }

    @Test
    void handleInvalidProcessState_withDifferentStates() {
        UUID processId = UUID.randomUUID();

        // Test with PENDING state
        InvalidProcessStateException exceptionPending = new InvalidProcessStateException(
                processId,
                ProcessStatus.PENDING,
                "stop"
        );
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responsePending =
                globalExceptionHandler.handleInvalidProcessState(exceptionPending);
        assertThat(responsePending.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responsePending.getBody().getStatus()).isEqualTo("PENDING");

        // Test with RUNNING state
        InvalidProcessStateException exceptionRunning = new InvalidProcessStateException(
                processId,
                ProcessStatus.RUNNING,
                "pause"
        );
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responseRunning =
                globalExceptionHandler.handleInvalidProcessState(exceptionRunning);
        assertThat(responseRunning.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseRunning.getBody().getStatus()).isEqualTo("RUNNING");

        // Test with PAUSED state
        InvalidProcessStateException exceptionPaused = new InvalidProcessStateException(
                processId,
                ProcessStatus.PAUSED,
                "resume"
        );
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> responsePaused =
                globalExceptionHandler.handleInvalidProcessState(exceptionPaused);
        assertThat(responsePaused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responsePaused.getBody().getStatus()).isEqualTo("PAUSED");
    }

    @Test
    void handleInvalidProcessState_shouldHaveCorrectStatusCode() {
        UUID processId = UUID.randomUUID();
        InvalidProcessStateException exception = new InvalidProcessStateException(
                processId,
                ProcessStatus.COMPLETED,
                "stop"
        );

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                globalExceptionHandler.handleInvalidProcessState(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}



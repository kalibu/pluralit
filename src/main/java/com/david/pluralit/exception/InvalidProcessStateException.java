package com.david.pluralit.exception;

import com.david.pluralit.model.enums.ProcessStatus;
import lombok.Getter;

import java.util.UUID;

@Getter
public class InvalidProcessStateException extends RuntimeException {
    private final UUID processId;
    private final ProcessStatus currentStatus;
    private final String attemptedAction;

    public InvalidProcessStateException(UUID processId, ProcessStatus currentStatus, String attemptedAction) {
        super(String.format("Cannot %s process. Current status: %s", attemptedAction, currentStatus));
        this.processId = processId;
        this.currentStatus = currentStatus;
        this.attemptedAction = attemptedAction;
    }
}

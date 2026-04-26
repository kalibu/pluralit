package com.david.pluralit.service;

import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.entity.ProcessLog;
import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.repository.ProcessLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessLogServiceTest {

    @Mock
    private ProcessLogRepository processLogRepository;

    @InjectMocks
    private ProcessLogService processLogService;

    private Process testProcess;
    private UUID testProcessId;

    @BeforeEach
    void setUp() {
        testProcessId = UUID.randomUUID();
        testProcess = new Process();
        testProcess.setId(testProcessId);
        testProcess.setStatus(ProcessStatus.RUNNING);
        testProcess.setStartedAt(Instant.now());
    }

    @Test
    void addProcessLog_shouldSaveLogWithCorrectMessage() {
        String message = "Process started";
        ProcessLog expectedLog = new ProcessLog();
        expectedLog.setProcess(testProcess);
        expectedLog.setMessage(message);

        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(expectedLog);

        processLogService.addProcessLog(testProcess, message);

        ArgumentCaptor<ProcessLog> logCaptor = ArgumentCaptor.forClass(ProcessLog.class);
        verify(processLogRepository, times(1)).save(logCaptor.capture());

        ProcessLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getProcess()).isEqualTo(testProcess);
        assertThat(savedLog.getMessage()).isEqualTo(message);
    }

    @Test
    void addProcessLog_shouldSaveMultipleLogMessages() {
        String message1 = "Process started";
        String message2 = "Processing file 1";
        String message3 = "Process completed";

        when(processLogRepository.save(any(ProcessLog.class)))
                .thenReturn(new ProcessLog())
                .thenReturn(new ProcessLog())
                .thenReturn(new ProcessLog());

        processLogService.addProcessLog(testProcess, message1);
        processLogService.addProcessLog(testProcess, message2);
        processLogService.addProcessLog(testProcess, message3);

        verify(processLogRepository, times(3)).save(any(ProcessLog.class));
    }

    @Test
    void addProcessLog_shouldHandleNullMessage() {
        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(new ProcessLog());

        processLogService.addProcessLog(testProcess, null);

        verify(processLogRepository, times(1)).save(any(ProcessLog.class));
    }

    @Test
    void addProcessLog_shouldHandleEmptyMessage() {
        String message = "";
        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(new ProcessLog());

        processLogService.addProcessLog(testProcess, message);

        ArgumentCaptor<ProcessLog> logCaptor = ArgumentCaptor.forClass(ProcessLog.class);
        verify(processLogRepository, times(1)).save(logCaptor.capture());

        ProcessLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getMessage()).isEmpty();
    }

    @Test
    void addProcessLog_shouldSetProcessCorrectly() {
        String message = "File processed";
        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(new ProcessLog());

        processLogService.addProcessLog(testProcess, message);

        ArgumentCaptor<ProcessLog> logCaptor = ArgumentCaptor.forClass(ProcessLog.class);
        verify(processLogRepository, times(1)).save(logCaptor.capture());

        ProcessLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getProcess().getId()).isEqualTo(testProcessId);
        assertThat(savedLog.getProcess().getStatus()).isEqualTo(ProcessStatus.RUNNING);
    }

    @Test
    void addProcessLog_shouldHandleLongMessage() {
        String longMessage = "This is a very long message that represents processing information. " +
                "It contains details about file processing, line counts, word counts, and other statistics. " +
                "This message tests that the service can handle lengthy log messages without issues. " +
                "It is important to ensure that all information is captured correctly in the database.";

        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(new ProcessLog());

        processLogService.addProcessLog(testProcess, longMessage);

        ArgumentCaptor<ProcessLog> logCaptor = ArgumentCaptor.forClass(ProcessLog.class);
        verify(processLogRepository, times(1)).save(logCaptor.capture());

        ProcessLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getMessage()).isEqualTo(longMessage);
    }

    @Test
    void addProcessLog_shouldHandleSpecialCharacters() {
        String messageWithSpecialChars = "Process error: File 'document.txt' failed with exception: NullPointerException at line 42";

        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(new ProcessLog());

        processLogService.addProcessLog(testProcess, messageWithSpecialChars);

        ArgumentCaptor<ProcessLog> logCaptor = ArgumentCaptor.forClass(ProcessLog.class);
        verify(processLogRepository, times(1)).save(logCaptor.capture());

        ProcessLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getMessage()).isEqualTo(messageWithSpecialChars);
    }

    @Test
    void addProcessLog_shouldSaveConcurrently() throws InterruptedException {
        when(processLogRepository.save(any(ProcessLog.class)))
                .thenReturn(new ProcessLog());

        Thread thread1 = new Thread(() ->
                processLogService.addProcessLog(testProcess, "Message from thread 1"));
        Thread thread2 = new Thread(() ->
                processLogService.addProcessLog(testProcess, "Message from thread 2"));

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        verify(processLogRepository, times(2)).save(any(ProcessLog.class));
    }

    @Test
    void addProcessLog_shouldPreserveTimestamp() {
        String message = "Process status update";
        ProcessLog expectedLog = new ProcessLog();
        expectedLog.setProcess(testProcess);
        expectedLog.setMessage(message);
        expectedLog.setCreatedAt(Instant.now());

        when(processLogRepository.save(any(ProcessLog.class))).thenReturn(expectedLog);

        processLogService.addProcessLog(testProcess, message);

        ArgumentCaptor<ProcessLog> logCaptor = ArgumentCaptor.forClass(ProcessLog.class);
        verify(processLogRepository, times(1)).save(logCaptor.capture());

        ProcessLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getMessage()).isEqualTo(message);
    }
}


package com.david.pluralit.service;

import com.david.pluralit.exception.InvalidProcessStateException;
import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.entity.ProcessResult;
import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.repository.ProcessRepository;
import com.david.pluralit.repository.ProcessResultRepository;
import com.david.pluralit.vo.ProcessVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessServiceUnitTest {

    @Mock
    private ProcessRepository processRepository;

    @Mock
    private ProcessResultRepository processResultRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AsyncProcessService asyncProcessService;

    @Mock
    private ProcessLogService processLogService;

    @InjectMocks
    private ProcessService processService;

    private UUID testProcessId;
    private Process testProcess;

    @BeforeEach
    void setUp() {
        testProcessId = UUID.randomUUID();
        testProcess = new Process();
        testProcess.setId(testProcessId);
        testProcess.setStatus(ProcessStatus.PENDING);
        testProcess.setStartedAt(Instant.now());
        testProcess.setTotalFiles(10);
        testProcess.setProcessedFiles(0);
        testProcess.setPercentage(0.0);
    }

    @Test
    void stopProcess_shouldUpdateStatusToStopped() {
        testProcess.setStatus(ProcessStatus.RUNNING);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));
        when(processRepository.save(any(Process.class))).thenReturn(testProcess);
        doNothing().when(processLogService).addProcessLog(any(Process.class), anyString());

        processService.stopProcess(testProcessId);

        verify(processRepository, times(1)).findById(testProcessId);
        verify(processRepository, times(1)).save(testProcess);
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), isA(ProcessVO.class));
    }

    @Test
    void stopProcess_shouldThrowExceptionWhenProcessCompleted() {
        testProcess.setStatus(ProcessStatus.COMPLETED);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));

        assertThatThrownBy(() -> processService.stopProcess(testProcessId))
                .isInstanceOf(InvalidProcessStateException.class);

        verify(processRepository, times(1)).findById(testProcessId);
        verify(processRepository, never()).save(any());
    }

    @Test
    void pauseProcess_shouldUpdateStatusToPaused() {
        testProcess.setStatus(ProcessStatus.RUNNING);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));
        when(processRepository.save(any(Process.class))).thenReturn(testProcess);
        doNothing().when(asyncProcessService).pauseProcess(testProcessId);
        doNothing().when(processLogService).addProcessLog(any(Process.class), anyString());

        processService.pauseProcess(testProcessId);

        verify(processRepository, times(1)).findById(testProcessId);
        verify(asyncProcessService, times(1)).pauseProcess(testProcessId);
        verify(processRepository, times(1)).save(testProcess);
    }

    @Test
    void resumeProcess_shouldUpdateStatusToRunning() {
        testProcess.setStatus(ProcessStatus.PAUSED);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));
        when(processRepository.save(any(Process.class))).thenReturn(testProcess);
        doNothing().when(asyncProcessService).resumeProcess(testProcessId);
        doNothing().when(asyncProcessService).processFilesAsync(testProcessId);
        doNothing().when(processLogService).addProcessLog(any(Process.class), anyString());

        processService.resumeProcess(testProcessId);

        verify(processRepository, times(1)).findById(testProcessId);
        verify(asyncProcessService, times(1)).resumeProcess(testProcessId);
        verify(asyncProcessService, times(1)).processFilesAsync(testProcessId);
    }

    @Test
    void resumeProcess_shouldThrowExceptionWhenNotPaused() {
        testProcess.setStatus(ProcessStatus.RUNNING);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));

        assertThatThrownBy(() -> processService.resumeProcess(testProcessId))
                .isInstanceOf(InvalidProcessStateException.class);

        verify(processRepository, times(1)).findById(testProcessId);
    }

    @Test
    void getStatus_shouldReturnProcessVO() {
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));

        ProcessVO vo = processService.getStatus(testProcessId);

        assertThat(vo).isNotNull();
        assertThat(vo.getProcessId()).isEqualTo(testProcessId);
        assertThat(vo.getStatus()).isEqualTo(ProcessStatus.PENDING);

        verify(processRepository, times(1)).findById(testProcessId);
    }

    @Test
    void listProcesses_shouldReturnAllProcesses() {
        Process process2 = new Process();
        process2.setId(UUID.randomUUID());
        process2.setStatus(ProcessStatus.COMPLETED);

        when(processRepository.findAll()).thenReturn(Arrays.asList(testProcess, process2));

        List<ProcessVO> vos = processService.listProcesses();

        assertThat(vos).hasSize(2);
        assertThat(vos.get(0).getProcessId()).isEqualTo(testProcessId);
        assertThat(vos.get(1).getStatus()).isEqualTo(ProcessStatus.COMPLETED);

        verify(processRepository, times(1)).findAll();
    }

    @Test
    void getResults_shouldReturnProcessWithResults() {
        ProcessResult result = new ProcessResult();
        result.setProcess(testProcess);
        result.setTotalWords(1500L);
        result.setTotalLines(75L);
        result.setMostFrequentWords("[\"the\", \"of\", \"and\"]");
        result.setFilesProcessed("[\"doc1.txt\", \"doc2.txt\"]");
        result.setSummary("Processed 2 files with 1500 words");

        testProcess.setStatus(ProcessStatus.COMPLETED);

        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));
        when(processResultRepository.findAll()).thenReturn(Arrays.asList(result));

        ProcessVO vo = processService.getResults(testProcessId);

        assertThat(vo).isNotNull();
        assertThat(vo.getResults()).isNotNull();
        assertThat(vo.getResults().getTotalWords()).isEqualTo(1500L);
        assertThat(vo.getResults().getTotalLines()).isEqualTo(75L);
        assertThat(vo.getResults().getSummary()).isEqualTo("Processed 2 files with 1500 words");

        verify(processRepository, times(1)).findById(testProcessId);
    }

    @Test
    void getResults_shouldReturnProcessWithoutResultsIfNone() {
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));
        when(processResultRepository.findAll()).thenReturn(Arrays.asList());

        ProcessVO vo = processService.getResults(testProcessId);

        assertThat(vo).isNotNull();
        assertThat(vo.getResults()).isNull();

        verify(processRepository, times(1)).findById(testProcessId);
    }

    @Test
    void stopProcess_shouldThrowExceptionWhenProcessFailed() {
        testProcess.setStatus(ProcessStatus.FAILED);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));

        assertThatThrownBy(() -> processService.stopProcess(testProcessId))
                .isInstanceOf(InvalidProcessStateException.class);
    }

    @Test
    void pauseProcess_shouldThrowExceptionWhenProcessCompleted() {
        testProcess.setStatus(ProcessStatus.COMPLETED);
        when(processRepository.findById(testProcessId)).thenReturn(Optional.of(testProcess));

        assertThatThrownBy(() -> processService.pauseProcess(testProcessId))
                .isInstanceOf(InvalidProcessStateException.class);
    }
}

@SpringBootTest
@ActiveProfiles("test")
class ProcessServiceIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ProcessRepository processRepository;

    @Test
    void startProcess_shouldCreateAndReturnProcessVO() throws InterruptedException {
        ProcessVO vo = processService.startProcess();

        assertThat(vo).isNotNull();
        assertThat(vo.getProcessId()).isNotNull();
        assertThat(vo.getStatus()).isIn(ProcessStatus.PENDING);

        Thread.sleep(100);

        Process process = processRepository.findById(vo.getProcessId()).orElse(null);
        assertThat(process).isNotNull();
        assertThat(process.getStatus()).isIn(ProcessStatus.PENDING);
    }

    @Test
    void listProcesses_shouldReturnList() {
        processService.startProcess();

        var list = processService.listProcesses();
        assertThat(list).isNotEmpty();
    }
}

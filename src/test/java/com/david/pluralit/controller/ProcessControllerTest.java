package com.david.pluralit.controller;

import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.service.ProcessService;
import com.david.pluralit.vo.ProcessVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessControllerTest {

    @Mock
    private ProcessService processService;

    @InjectMocks
    private ProcessController processController;

    private UUID testProcessId;
    private ProcessVO testProcessVO;

    @BeforeEach
    void setUp() {
        testProcessId = UUID.randomUUID();
        testProcessVO = new ProcessVO();
        testProcessVO.setProcessId(testProcessId);
        testProcessVO.setStatus(ProcessStatus.RUNNING);
        testProcessVO.setStartedAt(Instant.now());
        testProcessVO.setProgress(new ProcessVO.ProgressVO(10, 5, 50.0));
    }

    @Test
    void startProcess_shouldReturnProcessVO() {
        when(processService.startProcess()).thenReturn(testProcessVO);

        ResponseEntity<ProcessVO> response = processController.startProcess();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isEqualTo(testProcessId);
        assertThat(response.getBody().getStatus()).isEqualTo(ProcessStatus.RUNNING);

        verify(processService, times(1)).startProcess();
    }

    @Test
    void stopProcess_shouldCallServiceAndReturnOk() {
        doNothing().when(processService).stopProcess(testProcessId);

        ResponseEntity<Void> response = processController.stopProcess(testProcessId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(processService, times(1)).stopProcess(testProcessId);
    }

    @Test
    void pauseProcess_shouldCallServiceAndReturnOk() {
        doNothing().when(processService).pauseProcess(testProcessId);

        ResponseEntity<Void> response = processController.pauseProcess(testProcessId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(processService, times(1)).pauseProcess(testProcessId);
    }

    @Test
    void resumeProcess_shouldCallServiceAndReturnOk() {
        doNothing().when(processService).resumeProcess(testProcessId);

        ResponseEntity<Void> response = processController.resumeProcess(testProcessId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(processService, times(1)).resumeProcess(testProcessId);
    }

    @Test
    void getStatus_shouldReturnProcessVO() {
        when(processService.getStatus(testProcessId)).thenReturn(testProcessVO);

        ResponseEntity<ProcessVO> response = processController.getStatus(testProcessId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProcessId()).isEqualTo(testProcessId);

        verify(processService, times(1)).getStatus(testProcessId);
    }

    @Test
    void listProcesses_shouldReturnListOfProcessVOs() {
        List<ProcessVO> processes = Arrays.asList(testProcessVO);
        when(processService.listProcesses()).thenReturn(processes);

        ResponseEntity<List<ProcessVO>> response = processController.listProcesses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().size()).isEqualTo(1);
        assertThat(response.getBody().get(0).getProcessId()).isEqualTo(testProcessId);

        verify(processService, times(1)).listProcesses();
    }

    @Test
    void getResults_shouldReturnProcessVOWithResults() {
        ProcessVO.ResultsVO resultsVO = new ProcessVO.ResultsVO();
        resultsVO.setTotalWords(1500L);
        resultsVO.setTotalLines(75L);
        resultsVO.setMostFrequentWords(Arrays.asList("the", "of", "and"));
        resultsVO.setFilesProcessed(Arrays.asList("doc1.txt", "doc2.txt"));
        resultsVO.setSummary("Processed 2 files");

        testProcessVO.setResults(resultsVO);
        testProcessVO.setStatus(ProcessStatus.COMPLETED);

        when(processService.getResults(testProcessId)).thenReturn(testProcessVO);

        ResponseEntity<ProcessVO> response = processController.getResults(testProcessId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResults()).isNotNull();
        assertThat(response.getBody().getResults().getTotalWords()).isEqualTo(1500L);
        assertThat(response.getBody().getResults().getMostFrequentWords()).containsExactly("the", "of", "and");

        verify(processService, times(1)).getResults(testProcessId);
    }

    @Test
    void listProcesses_shouldReturnEmptyListWhenNoProcesses() {
        when(processService.listProcesses()).thenReturn(Arrays.asList());

        ResponseEntity<List<ProcessVO>> response = processController.listProcesses();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();

        verify(processService, times(1)).listProcesses();
    }
}


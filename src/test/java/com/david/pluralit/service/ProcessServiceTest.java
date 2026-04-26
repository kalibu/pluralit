package com.david.pluralit.service;

import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.repository.ProcessRepository;
import com.david.pluralit.vo.ProcessVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProcessServiceTest {

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

        // Wait a bit for async processing
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

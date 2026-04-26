package com.david.pluralit.service;

import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.entity.ProcessLog;
import com.david.pluralit.repository.ProcessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessLogService {

    private final ProcessLogRepository processLogRepository;

    public void addProcessLog(Process process, String message) {
        ProcessLog logEntry = new ProcessLog();
        logEntry.setProcess(process);
        logEntry.setMessage(message);
        processLogRepository.save(logEntry);
    }
}

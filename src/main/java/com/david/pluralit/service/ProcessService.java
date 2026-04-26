package com.david.pluralit.service;

import com.david.pluralit.exception.InvalidProcessStateException;
import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.entity.ProcessResult;
import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.repository.ProcessRepository;
import com.david.pluralit.repository.ProcessResultRepository;
import com.david.pluralit.vo.ProcessVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessService {

    private final ProcessRepository processRepository;
    private final ProcessResultRepository processResultRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AsyncProcessService asyncProcessService;
    private final ProcessLogService processLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pluralit.file-folder}")
    private String fileFolder;

    @Transactional
    public ProcessVO startProcess() {
        log.info("🎯 Starting new process...");
        Process process = new Process();
        process.setStatus(ProcessStatus.PENDING);
        process = processRepository.save(process);
        processRepository.flush();
        log.info("✅ New process created with ID: {}", process.getId());

        processLogService.addProcessLog(process, "Process started");

        // Estimate completion based on file count
        try {
            long fileCount = Files.list(Paths.get(fileFolder)).filter(Files::isRegularFile).count();
            process.setTotalFiles((int) fileCount);
            process.setEstimatedCompletion(Instant.now().plusSeconds(fileCount * 10)); // rough estimate
            processRepository.save(process);
            log.info("📊 Estimated {} files, completion time: {}", fileCount, process.getEstimatedCompletion());
        } catch (IOException e) {
            log.error("❌ Error counting files in folder: {} - {}", fileFolder, e.getMessage(), e);
            process.setStatus(ProcessStatus.FAILED);
            process = processRepository.save(process);
        }

        UUID processId = process.getId();
        log.info("🚀 Triggering async file processing for processId: {}", processId);
        asyncProcessService.processFilesAsync(processId);

        return process.toVO();
    }


    @Transactional
    public void stopProcess(UUID processId) {
        log.info("🛑 Stopping process with ID: {}", processId);
        Process process = processRepository.findById(processId).orElseThrow();
        if (process.getStatus() == ProcessStatus.COMPLETED || process.getStatus() == ProcessStatus.FAILED) {
            throw new InvalidProcessStateException(processId, process.getStatus(), "stop");
        }
        process.setStatus(ProcessStatus.STOPPED);
        process.setCompletedAt(Instant.now());
        processRepository.save(process);
        log.info("✅ Process stopped and persisted for processId: {}", processId);
        processLogService.addProcessLog(process, "Process stopped");
        sendUpdate(process);
    }

    @Transactional
    public void pauseProcess(UUID processId) {
        log.info("⏸️  Pausing process with ID: {}", processId);
        Process process = processRepository.findById(processId).orElseThrow();
        if (process.getStatus() == ProcessStatus.COMPLETED || process.getStatus() == ProcessStatus.FAILED) {
            throw new InvalidProcessStateException(processId, process.getStatus(), "pause");
        }
        asyncProcessService.pauseProcess(processId);
        process.setStatus(ProcessStatus.PAUSED);
        processRepository.save(process);
        log.info("✅ Process paused and persisted for processId: {}", processId);
        processLogService.addProcessLog(process, "Process paused");
        sendUpdate(process);
    }

    @Transactional
    public void resumeProcess(UUID processId) {
        log.info("▶️  Resuming process with ID: {}", processId);
        Process process = processRepository.findById(processId).orElseThrow();
        if (process.getStatus() != ProcessStatus.PAUSED) {
            throw new InvalidProcessStateException(processId, process.getStatus(), "resume");
        }
        asyncProcessService.resumeProcess(processId);
        process.setStatus(ProcessStatus.RUNNING);
        processRepository.save(process);
        log.info("✅ Process resumed and persisted for processId: {}", processId);
        processLogService.addProcessLog(process, "Process resumed");
        asyncProcessService.processFilesAsync(processId);
        sendUpdate(process);
    }

    public ProcessVO getStatus(UUID processId) {
        log.debug("🔍 Fetching status for processId: {}", processId);
        Process process = processRepository.findById(processId).orElseThrow();
        log.debug("✅ Process status retrieved: Status={}, Progress={}%", process.getStatus(), process.getPercentage());
        return process.toVO();
    }

    public List<ProcessVO> listProcesses() {
        log.debug("📋 Listing all processes...");
        List<ProcessVO> processes = processRepository.findAll().stream()
                .map(Process::toVO)
                .collect(Collectors.toList());
        log.info("✅ Retrieved {} processes", processes.size());
        return processes;
    }

    public ProcessVO getResults(UUID processId) {
        log.info("📊 Fetching results for processId: {}", processId);
        Process process = processRepository.findById(processId).orElseThrow();
        ProcessResult result = processResultRepository.findAll().stream()
                .filter(r -> r.getProcess().getId().equals(processId))
                .findFirst().orElse(null);
        ProcessVO vo = process.toVO();
        if (result != null) {
            log.info("✅ Results found for processId: {} - Files: {}, Words: {}, Lines: {}", processId, result.getFilesProcessed(), result.getTotalWords(), result.getTotalLines());
            ProcessVO.ResultsVO res = new ProcessVO.ResultsVO();
            res.setTotalWords(result.getTotalWords());
            res.setTotalLines(result.getTotalLines());
            try {
                res.setMostFrequentWords(objectMapper.readValue(result.getMostFrequentWords(), new TypeReference<>() {}));
                res.setFilesProcessed(objectMapper.readValue(result.getFilesProcessed(), new TypeReference<>() {}));
            } catch (Exception e) {
                log.error("❌ Error parsing JSON results for processId: {} - {}", processId, e.getMessage(), e);
            }
            res.setSummary(result.getSummary());
            vo.setResults(res);
        } else {
            log.warn("⚠️  No results found for processId: {}", processId);
        }
        return vo;
    }

    private void sendUpdate(Process process) {
        messagingTemplate.convertAndSend("/topic/process/" + process.getId(), process.toVO());
    }

}

package com.david.pluralit.service;

import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.entity.ProcessResult;
import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.repository.ProcessRepository;
import com.david.pluralit.repository.ProcessResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncProcessService {

    private final ProcessRepository processRepository;
    private final ProcessResultRepository processResultRepository;
    private final ProcessLogService processLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${pluralit.file-folder}")
    private String fileFolder;

    @Value("${pluralit.batch-size:5}")
    private int batchSize;

    private final Map<UUID, Boolean> pauseFlags = new ConcurrentHashMap<>();

    /**
     * Executes file processing asynchronously.
     * This method runs in a separate thread and should NOT be called directly from ProcessService.
     */
    @Async("taskExecutor")
    public void processFilesAsync(UUID processId) {
        log.info("🚀 Starting asynchronous file processing for processId: {}", processId);
        Process process = processRepository.findById(processId).orElse(null);
        if (process == null) {
            log.error("❌ Process not found with ID: {}", processId);
            return;
        }

        process.setStatus(ProcessStatus.RUNNING);
        process.setStartedAt(Instant.now());
        processRepository.save(process);
        log.info("✅ Process status updated to RUNNING for processId: {}", processId);

        try {
            Path folder = Paths.get(fileFolder);
            log.info("📁 Reading files from folder: {}", fileFolder);
            List<Path> files = Files.list(folder)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList();

            process.setTotalFiles(files.size());
            processRepository.save(process);
            log.info("📊 Found {} .txt files to process for processId: {}", files.size(), processId);

            List<String> processedFiles = new ArrayList<>();
            Map<String, Long> wordFreq = new HashMap<>();
            long totalWords = 0;
            long totalLines = 0;
            long totalChars = 0;

            for (int i = 0; i < files.size(); i += batchSize) {
                if (pauseFlags.getOrDefault(processId, false)) {
                    log.warn("⏸️  Process paused at batch {}/{} for processId: {}", (i / batchSize) + 1, (files.size() / batchSize) + 1, processId);
                    process.setStatus(ProcessStatus.PAUSED);
                    processRepository.save(process);
                    sendUpdate(process);
                    return;
                }

                List<Path> batch = files.subList(i, Math.min(i + batchSize, files.size()));
                log.debug("📦 Processing batch {}/{} with {} files for processId: {}", (i / batchSize) + 1, (files.size() / batchSize) + 1, batch.size(), processId);

                for (Path file : batch) {
                    if (process.getStatus() == ProcessStatus.STOPPED) {
                        log.warn("🛑 Process stopped during file processing for processId: {}", processId);
                        return;
                    }

                    try {
                        log.debug("📄 Reading file: {}", file.getFileName());
                        String content = Files.readString(file);
                        String[] words = content.split("\\s+");
                        totalWords += words.length;
                        totalLines += content.lines().count();
                        totalChars += content.length();

                        for (String word : words) {
                            word = word.toLowerCase().replaceAll("[^a-zA-Z]", "");
                            if (!word.isEmpty()) {
                                wordFreq.put(word, wordFreq.getOrDefault(word, 0L) + 1);
                            }
                        }

                        processedFiles.add(file.getFileName().toString());
                        process.setProcessedFiles(processedFiles.size());
                        process.setPercentage((double) processedFiles.size() / files.size() * 100);
                        processRepository.save(process);
                        sendUpdate(process);

                        log.info("✓ File processed: {} | Progress: {}/{} ({}%)", file.getFileName(), processedFiles.size(), files.size(), process.getPercentage());
                        processLogService.addProcessLog(process, "Processed file: " + file.getFileName());

                    } catch (IOException e) {
                        log.error("❌ Error processing file: {} - {}", file, e.getMessage(), e);
                        processLogService.addProcessLog(process, "Error processing file: " + file);
                    }
                }

                // Simulate processing time
                log.debug("⏳ Waiting 1 second before next batch...");
                Thread.sleep(1000);
            }

            // Generate results
            log.info("📈 Generating results for processId: {}", processId);
            log.info("📊 Statistics - Total Words: {}, Total Lines: {}, Total Characters: {}", totalWords, totalLines, totalChars);

            List<String> topWords = wordFreq.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            log.info("🔤 Top 5 most frequent words: {}", topWords);

            String summary = "Processed " + processedFiles.size() + " files with " + totalWords + " words.";

            ProcessResult result = new ProcessResult();
            result.setProcess(process);
            result.setTotalWords(totalWords);
            result.setTotalLines(totalLines);
            result.setTotalCharacters(totalChars);
            result.setMostFrequentWords(objectMapper.writeValueAsString(topWords));
            result.setFilesProcessed(objectMapper.writeValueAsString(processedFiles));
            result.setSummary(summary);
            processResultRepository.save(result);
            log.info("💾 Results saved to database for processId: {}", processId);

            process.setStatus(ProcessStatus.COMPLETED);
            process.setCompletedAt(Instant.now());
            processRepository.save(process);
            sendUpdate(process);
            log.info("✅ Process COMPLETED successfully for processId: {}", processId);
            processLogService.addProcessLog(process, "Process completed");

        } catch (Exception e) {
            log.error("❌ Critical error in process execution for processId: {} - Error: {}", processId, e.getMessage(), e);
            process.setStatus(ProcessStatus.FAILED);
            processRepository.save(process);
            sendUpdate(process);
            processLogService.addProcessLog(process, "Process failed: " + e.getMessage());
        } finally {
            pauseFlags.remove(processId);
            log.info("🏁 Cleanup completed for processId: {}", processId);
        }
    }

    @Async("taskExecutor")
    public void pauseProcess(UUID processId) {
        log.info("⏸️  Flagging process {} for pause", processId);
        pauseFlags.put(processId, true);
    }

    @Async("taskExecutor")
    public void resumeProcess(UUID processId) {
        log.info("▶️  Removing pause flag for process {}", processId);
        pauseFlags.remove(processId);
    }

    private void sendUpdate(Process process) {
        messagingTemplate.convertAndSend("/topic/process/" + process.getId(), process.toVO());
    }
}


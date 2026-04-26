package com.david.pluralit.service;

import com.david.pluralit.model.entity.Process;
import com.david.pluralit.model.entity.ProcessResult;
import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.repository.ProcessRepository;
import com.david.pluralit.repository.ProcessResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
        
        Process process = findAndValidateProcess(processId);
        if (process == null) return;
        
        initializeProcessExecution(process);
        
        try {
            List<Path> files = discoverTextFiles();
            updateProcessWithFileCount(process, files.size());
            
            ProcessingContext context = new ProcessingContext();
            processBatches(process, files, context);
            
            if (process.getStatus() != ProcessStatus.STOPPED && process.getStatus() != ProcessStatus.PAUSED) {
                completeProcessing(process, context);
            }
            
        } catch (Exception e) {
            handleProcessingError(process, e);
        } finally {
            cleanupProcess(processId);
        }
    }
    
    private Process findAndValidateProcess(UUID processId) {
        Process process = processRepository.findById(processId).orElse(null);
        if (process == null) {
            log.error("❌ Process not found with ID: {}", processId);
        }
        return process;
    }
    
    private void initializeProcessExecution(Process process) {
        process.setStatus(ProcessStatus.RUNNING);
        process.setStartedAt(Instant.now());
        processRepository.save(process);
        log.info("✅ Process status updated to RUNNING for processId: {}", process.getId());
    }
    
    private List<Path> discoverTextFiles() throws IOException {
        Path folder = Paths.get(fileFolder);
        log.info("📁 Reading files from folder: {}", fileFolder);
        
        return Files.list(folder)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .toList();
    }
    
    private void updateProcessWithFileCount(Process process, int fileCount) {
        process.setTotalFiles(fileCount);
        processRepository.save(process);
        log.info("📊 Found {} .txt files to process for processId: {}", fileCount, process.getId());
    }
    
    private void processBatches(Process process, List<Path> files, ProcessingContext context) throws InterruptedException {
        for (int i = 0; i < files.size(); i += batchSize) {
            if (shouldPauseProcess(process.getId(), i, files.size())) {
                pauseProcessExecution(process);
                return;
            }
            
            List<Path> batch = files.subList(i, Math.min(i + batchSize, files.size()));
            processBatch(process, batch, files.size(), context);
            
            simulateProcessingDelay();
        }
    }
    
    private boolean shouldPauseProcess(UUID processId, int currentIndex, int totalFiles) {
        if (pauseFlags.getOrDefault(processId, false)) {
            log.warn("⏸️  Process paused at batch {}/{} for processId: {}", 
                    (currentIndex / batchSize) + 1, (totalFiles / batchSize) + 1, processId);
            return true;
        }
        return false;
    }
    
    private void pauseProcessExecution(Process process) {
        process.setStatus(ProcessStatus.PAUSED);
        processRepository.save(process);
        sendUpdate(process);
    }
    
    private void processBatch(Process process, List<Path> batch, int totalFiles, ProcessingContext context) {
        log.debug("📦 Processing batch with {} files for processId: {}", batch.size(), process.getId());
        
        for (Path file : batch) {
            if (process.getStatus() == ProcessStatus.STOPPED) {
                log.warn("🛑 Process stopped during file processing for processId: {}", process.getId());
                return;
            }
            
            processFile(process, file, totalFiles, context);
        }
    }
    
    private void processFile(Process process, Path file, int totalFiles, ProcessingContext context) {
        try {
            log.debug("📄 Reading file: {}", file.getFileName());
            String content = Files.readString(file);
            
            FileStatistics stats = analyzeFileContent(content);
            context.addFileStatistics(stats, file.getFileName().toString());
            
            updateProcessProgress(process, file, totalFiles, context.getProcessedFileCount());
            
        } catch (IOException e) {
            handleFileProcessingError(process, file, e);
        }
    }
    
    private FileStatistics analyzeFileContent(String content) {
        String[] words = content.split("\\s+");
        Map<String, Long> wordFreq = new HashMap<>();
        
        for (String word : words) {
            String cleanWord = sanitizeWord(word);
            if (!cleanWord.isEmpty()) {
                wordFreq.put(cleanWord, wordFreq.getOrDefault(cleanWord, 0L) + 1);
            }
        }
        
        return new FileStatistics(
            words.length,
            content.lines().count(),
            content.length(),
            wordFreq
        );
    }
    
    private String sanitizeWord(String word) {
        return word.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z]", "");
    }
    
    private void updateProcessProgress(Process process, Path file, int totalFiles, int processedCount) {
        process.setProcessedFiles(processedCount);
        process.setPercentage((double) processedCount / totalFiles * 100);
        processRepository.save(process);
        sendUpdate(process);
        
        log.info("✓ File processed: {} | Progress: {}/{} ({}%)", 
                file.getFileName(), processedCount, totalFiles, process.getPercentage());
        processLogService.addProcessLog(process, "Processed file: " + file.getFileName());
    }
    
    private void handleFileProcessingError(Process process, Path file, IOException e) {
        log.error("❌ Error processing file: {} - {}", file, e.getMessage(), e);
        processLogService.addProcessLog(process, "Error processing file: " + file);
    }
    
    private void simulateProcessingDelay() throws InterruptedException {
        log.debug("⏳ Waiting 1 second before next batch...");
        Thread.sleep(1000);
    }
    
    private void completeProcessing(Process process, ProcessingContext context) throws Exception {
        log.info("📈 Generating results for processId: {}", process.getId());
        
        ProcessResult result = generateProcessResult(process, context);
        processResultRepository.save(result);
        
        finalizeProcess(process);
    }
    
    private ProcessResult generateProcessResult(Process process, ProcessingContext context) throws Exception {
        List<String> topWords = context.getTopFrequentWords(5);
        String summary = generateSummary(context);
        
        log.info("📊 Statistics - Total Words: {}, Total Lines: {}, Total Characters: {}", 
                context.getTotalWords(), context.getTotalLines(), context.getTotalCharacters());
        log.info("🔤 Top 5 most frequent words: {}", topWords);
        
        ProcessResult result = new ProcessResult();
        result.setProcess(process);
        result.setTotalWords(context.getTotalWords());
        result.setTotalLines(context.getTotalLines());
        result.setTotalCharacters(context.getTotalCharacters());
        result.setMostFrequentWords(objectMapper.writeValueAsString(topWords));
        result.setFilesProcessed(objectMapper.writeValueAsString(context.getProcessedFileNames()));
        result.setSummary(summary);
        
        return result;
    }
    
    private String generateSummary(ProcessingContext context) {
        return "Processed " + context.getProcessedFileCount() + " files with " + context.getTotalWords() + " words.";
    }
    
    private void finalizeProcess(Process process) {
        process.setStatus(ProcessStatus.COMPLETED);
        process.setCompletedAt(Instant.now());
        processRepository.save(process);
        sendUpdate(process);
        
        log.info("✅ Process COMPLETED successfully for processId: {}", process.getId());
        processLogService.addProcessLog(process, "Process completed");
        log.info("💾 Results saved to database for processId: {}", process.getId());
    }
    
    private void handleProcessingError(Process process, Exception e) {
        log.error("❌ Critical error in process execution for processId: {} - Error: {}", 
                process.getId(), e.getMessage(), e);
        process.setStatus(ProcessStatus.FAILED);
        processRepository.save(process);
        sendUpdate(process);
        processLogService.addProcessLog(process, "Process failed: " + e.getMessage());
    }
    
    private void cleanupProcess(UUID processId) {
        pauseFlags.remove(processId);
        log.info("🏁 Cleanup completed for processId: {}", processId);
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
    
    // Inner classes for better organization
    private static class ProcessingContext {
        private final List<String> processedFileNames = new ArrayList<>();
        private final Map<String, Long> globalWordFrequency = new HashMap<>();
        @Getter
        private long totalWords = 0;
        @Getter
        private long totalLines = 0;
        @Getter
        private long totalCharacters = 0;
        
        public void addFileStatistics(FileStatistics stats, String fileName) {
            totalWords += stats.wordCount();
            totalLines += stats.lineCount();
            totalCharacters += stats.characterCount();
            
            // Merge word frequencies
            stats.wordFrequency().forEach((word, count) -> 
                globalWordFrequency.merge(word, count, Long::sum));
            
            processedFileNames.add(fileName);
        }
        
        public List<String> getTopFrequentWords(int limit) {
            return globalWordFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
        
        public int getProcessedFileCount() {
            return processedFileNames.size();
        }
        
        public List<String> getProcessedFileNames() {
            return new ArrayList<>(processedFileNames);
        }

    }
    
    private record FileStatistics(
        long wordCount,
        long lineCount, 
        long characterCount,
        Map<String, Long> wordFrequency
    ) {}
}


package com.david.pluralit.controller;

import com.david.pluralit.service.ProcessService;
import com.david.pluralit.vo.ProcessVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
@Tag(name = "Process Control API", description = "APIs for managing text file processing")
public class ProcessController {

    private final ProcessService processService;

    @PostMapping("/start")
    @Operation(summary = "Start a new analysis process")
    public ResponseEntity<ProcessVO> startProcess() {
        ProcessVO vo = processService.startProcess();
        return ResponseEntity.ok(vo);
    }

    @PostMapping("/stop/{process_id}")
    @Operation(summary = "Stop a specific process")
    public ResponseEntity<Void> stopProcess(@PathVariable("process_id") UUID processId) {
        processService.stopProcess(processId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pause/{process_id}")
    @Operation(summary = "Pause a specific process")
    public ResponseEntity<Void> pauseProcess(@PathVariable("process_id") UUID processId) {
        processService.pauseProcess(processId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resume/{process_id}")
    @Operation(summary = "Resume a specific process")
    public ResponseEntity<Void> resumeProcess(@PathVariable("process_id") UUID processId) {
        processService.resumeProcess(processId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{process_id}")
    @Operation(summary = "Query the status of a process")
    public ResponseEntity<ProcessVO> getStatus(@PathVariable("process_id") UUID processId) {
        ProcessVO vo = processService.getStatus(processId);
        return ResponseEntity.ok(vo);
    }

    @GetMapping("/list")
    @Operation(summary = "List all processes and their states")
    public ResponseEntity<List<ProcessVO>> listProcesses() {
        List<ProcessVO> vos = processService.listProcesses();
        return ResponseEntity.ok(vos);
    }

    @GetMapping("/results/{process_id}")
    @Operation(summary = "Get analysis results")
    public ResponseEntity<ProcessVO> getResults(@PathVariable("process_id") UUID processId) {
        ProcessVO vo = processService.getResults(processId);
        return ResponseEntity.ok(vo);
    }
}

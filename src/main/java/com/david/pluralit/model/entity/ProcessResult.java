package com.david.pluralit.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "process_results")
@Data
public class ProcessResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "process_id")
    private Process process;

    private Long totalWords;

    private Long totalLines;

    private Long totalCharacters;

    @Column(columnDefinition = "TEXT")
    private String mostFrequentWords; // JSON string

    @Column(columnDefinition = "TEXT")
    private String filesProcessed; // JSON string

    @Column(columnDefinition = "TEXT")
    private String summary;
}

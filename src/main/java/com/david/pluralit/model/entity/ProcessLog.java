package com.david.pluralit.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "process_logs")
@Data
public class ProcessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "process_id")
    private Process process;

    private String message;

    @CreationTimestamp
    private Instant createdAt;
}

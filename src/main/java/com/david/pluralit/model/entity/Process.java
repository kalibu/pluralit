package com.david.pluralit.model.entity;

import com.david.pluralit.model.enums.ProcessStatus;
import com.david.pluralit.vo.ProcessVO;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processes")
@Data
public class Process {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private ProcessStatus status = ProcessStatus.PENDING;

    @CreationTimestamp
    private Instant startedAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant completedAt;

    private Integer totalFiles;

    private Integer processedFiles = 0;

    private Double percentage = 0.0;

    private Instant estimatedCompletion;

    public ProcessVO toVO() {
        ProcessVO vo = new ProcessVO();
        vo.setProcessId(id);
        vo.setStatus(status);
        vo.setStartedAt(startedAt);
        vo.setEstimatedCompletion(estimatedCompletion);
        vo.setProgress(new ProcessVO.ProgressVO(totalFiles, processedFiles, percentage));
        return vo;
    }
}

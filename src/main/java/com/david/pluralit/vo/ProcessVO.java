package com.david.pluralit.vo;

import com.david.pluralit.model.enums.ProcessStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProcessVO {

    private UUID processId;

    private ProcessStatus status;

    private ProgressVO progress;

    private Instant startedAt;

    private Instant estimatedCompletion;

    private ResultsVO results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressVO {
        private Integer totalFiles;
        private Integer processedFiles;
        private Double percentage;
    }

    @Data
    public static class ResultsVO {
        private Long totalWords;
        private Long totalLines;
        private List<String> mostFrequentWords;
        private List<String> filesProcessed;
        private String summary;
    }
}

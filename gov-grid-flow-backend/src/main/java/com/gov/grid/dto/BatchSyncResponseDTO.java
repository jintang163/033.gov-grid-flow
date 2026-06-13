package com.gov.grid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncResponseDTO {

    private Boolean success;

    private String message;

    private Integer totalCount;

    private Integer successCount;

    private Integer failedCount;

    private Integer duplicateCount;

    @Builder.Default
    private List<SyncResultItem> results = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncResultItem {
        private String clientId;
        private Long serverId;
        private Boolean success;
        private Boolean duplicate;
        private String error;
    }
}

package com.gov.grid.vo;

import lombok.Data;

@Data
public class CommunityRankVO {

    private Long gridId;

    private String gridName;

    private Long totalCount;

    private Long completedCount;

    private Double completionRate;
}

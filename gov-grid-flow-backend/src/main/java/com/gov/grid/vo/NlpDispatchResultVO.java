package com.gov.grid.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NlpDispatchResultVO {

    private Long dispatchRecordId;

    private String departmentCode;

    private String departmentName;

    private BigDecimal confidence;

    private Boolean autoDispatch;

    private String method;

    private List<DeptScoreVO> allScores;

    @Data
    public static class DeptScoreVO {
        private String departmentCode;
        private String departmentName;
        private BigDecimal score;
    }
}

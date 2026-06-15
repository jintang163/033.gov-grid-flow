package com.gov.grid.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PublicOpinionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal opinionIndex;

    private String opinionLevel;

    private BigDecimal avgSentimentScore;

    private Integer totalEvaluations;

    private Integer positiveCount;

    private Integer negativeCount;

    private Integer neutralCount;

    private Integer warningCount;

    private BigDecimal positiveRate;

    private BigDecimal negativeRate;

    private BigDecimal avgSpeedScore;

    private BigDecimal avgEffectScore;

    private List<OpinionTrendVO> trendData;

    private List<WordCloudVO> wordCloud;

    private List<HotEventVO> hotNegativeEvents;

    private List<GridOpinionVO> gridOpinions;

    @Data
    public static class OpinionTrendVO implements Serializable {
        private String date;
        private BigDecimal opinionIndex;
        private BigDecimal avgSentimentScore;
        private Integer evaluationCount;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class WordCloudVO implements Serializable {
        private String name;
        private Integer value;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class HotEventVO implements Serializable {
        private Long eventId;
        private String eventTitle;
        private String eventType;
        private String sentimentLabel;
        private Double sentimentScore;
        private Integer speedScore;
        private Integer effectScore;
        private String content;
        private String gridName;
        private String warningLevel;
        private String createdAt;
        private static final long serialVersionUID = 1L;
    }

    @Data
    public static class GridOpinionVO implements Serializable {
        private Long gridId;
        private String gridName;
        private BigDecimal opinionIndex;
        private Integer evaluationCount;
        private Integer warningCount;
        private static final long serialVersionUID = 1L;
    }
}

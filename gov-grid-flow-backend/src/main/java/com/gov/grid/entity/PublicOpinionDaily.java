package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("public_opinion_daily")
public class PublicOpinionDaily extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("stat_date")
    private String statDate;

    @TableField("grid_id")
    private Long gridId;

    @TableField("grid_name")
    private String gridName;

    @TableField("total_evaluations")
    private Integer totalEvaluations;

    @TableField("positive_count")
    private Integer positiveCount;

    @TableField("negative_count")
    private Integer negativeCount;

    @TableField("neutral_count")
    private Integer neutralCount;

    @TableField("avg_sentiment_score")
    private BigDecimal avgSentimentScore;

    @TableField("opinion_index")
    private BigDecimal opinionIndex;

    @TableField("avg_speed_score")
    private BigDecimal avgSpeedScore;

    @TableField("avg_effect_score")
    private BigDecimal avgEffectScore;

    @TableField("warning_count")
    private Integer warningCount;

    @TableField("top_keywords")
    private String topKeywords;
}

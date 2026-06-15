package com.gov.grid.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.dto.PublicOpinionQueryDTO;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.PublicOpinionDaily;
import com.gov.grid.mapper.EventEvaluationMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.service.PublicOpinionService;
import com.gov.grid.service.SentimentAnalysisService;
import com.gov.grid.service.WordCloudService;
import com.gov.grid.vo.PublicOpinionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicOpinionServiceImpl implements PublicOpinionService {

    private final EventEvaluationMapper evaluationMapper;
    private final EventInfoMapper eventInfoMapper;
    private final GridInfoMapper gridInfoMapper;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final WordCloudService wordCloudService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PublicOpinionVO getOpinionDashboard(PublicOpinionQueryDTO queryDTO) {
        PublicOpinionVO vo = new PublicOpinionVO();

        LocalDate endDate = queryDTO.getEndDate() != null ?
                LocalDate.parse(queryDTO.getEndDate(), DATE_FORMATTER) : LocalDate.now();
        LocalDate startDate = queryDTO.getStartDate() != null ?
                LocalDate.parse(queryDTO.getStartDate(), DATE_FORMATTER) :
                endDate.minusDays(Math.max(6, queryDTO.getTrendDays() - 1));

        String startStr = startDate.format(DATE_FORMATTER) + " 00:00:00";
        String endStr = endDate.format(DATE_FORMATTER) + " 23:59:59";
        Long gridId = queryDTO.getGridId();

        List<EventEvaluation> evaluations = evaluationMapper.selectByDateRangeAndGrid(startStr, endStr, gridId);

        calculateBasicStats(vo, evaluations);

        vo.setTrendData(calculateTrendData(evaluations, startDate, endDate, gridId));

        List<String> allTexts = evaluations.stream()
                .map(EventEvaluation::getContent)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        vo.setWordCloud(calculateWordCloud(allTexts, queryDTO.getWordCloudSize()));

        vo.setHotNegativeEvents(getHotNegativeEvents(startStr, gridId, queryDTO.getHotEventSize()));

        vo.setGridOpinions(getGridOpinions(evaluations));

        return vo;
    }

    @Override
    public PublicOpinionVO getGridOpinion(Long gridId, PublicOpinionQueryDTO queryDTO) {
        queryDTO.setGridId(gridId);
        return getOpinionDashboard(queryDTO);
    }

    private void calculateBasicStats(PublicOpinionVO vo, List<EventEvaluation> evaluations) {
        int total = evaluations.size();
        int positiveCount = 0;
        int negativeCount = 0;
        int neutralCount = 0;
        double totalSentiment = 0;
        int validSentimentCount = 0;
        double totalSpeed = 0;
        int speedCount = 0;
        double totalEffect = 0;
        int effectCount = 0;
        int warningCount = 0;

        for (EventEvaluation eval : evaluations) {
            String label = eval.getSentimentLabel();
            if ("positive".equals(label)) {
                positiveCount++;
            } else if ("negative".equals(label)) {
                negativeCount++;
            } else {
                neutralCount++;
            }

            if (eval.getSentimentScore() != null) {
                totalSentiment += eval.getSentimentScore();
                validSentimentCount++;
            }

            if (eval.getSpeedScore() != null) {
                totalSpeed += eval.getSpeedScore();
                speedCount++;
            }
            if (eval.getEffectScore() != null) {
                totalEffect += eval.getEffectScore();
                effectCount++;
            }

            if ("critical".equals(eval.getWarningLevel()) || "warning".equals(eval.getWarningLevel())) {
                warningCount++;
            }
        }

        vo.setTotalEvaluations(total);
        vo.setPositiveCount(positiveCount);
        vo.setNegativeCount(negativeCount);
        vo.setNeutralCount(neutralCount);

        BigDecimal avgSentiment = validSentimentCount > 0 ?
                BigDecimal.valueOf(totalSentiment / validSentimentCount)
                        .setScale(4, RoundingMode.HALF_UP) :
                BigDecimal.valueOf(0.5);
        vo.setAvgSentimentScore(avgSentiment);

        BigDecimal opinionIndex = calculateOpinionIndex(avgSentiment.doubleValue(),
                speedCount > 0 ? totalSpeed / speedCount : 3.0,
                effectCount > 0 ? totalEffect / effectCount : 3.0,
                total, positiveCount, negativeCount);
        vo.setOpinionIndex(opinionIndex);

        vo.setOpinionLevel(getOpinionLevel(opinionIndex.doubleValue()));

        vo.setPositiveRate(total > 0 ?
                BigDecimal.valueOf((double) positiveCount / total).setScale(4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);
        vo.setNegativeRate(total > 0 ?
                BigDecimal.valueOf((double) negativeCount / total).setScale(4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        vo.setAvgSpeedScore(speedCount > 0 ?
                BigDecimal.valueOf(totalSpeed / speedCount).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);
        vo.setAvgEffectScore(effectCount > 0 ?
                BigDecimal.valueOf(totalEffect / effectCount).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        vo.setWarningCount(warningCount);
    }

    private BigDecimal calculateOpinionIndex(double sentimentScore, double avgSpeed, double avgEffect,
                                              int total, int positive, int negative) {
        double sentimentWeight = 0.5;
        double speedWeight = 0.2;
        double effectWeight = 0.2;
        double rateWeight = 0.1;

        double sentimentNorm = sentimentScore;
        double speedNorm = avgSpeed / 5.0;
        double effectNorm = avgEffect / 5.0;

        double positiveRate = total > 0 ? (double) positive / total : 0.5;
        double rateScore = 0.5 + (positiveRate - 0.5) * 0.5;

        double index = sentimentNorm * sentimentWeight
                + speedNorm * speedWeight
                + effectNorm * effectWeight
                + rateScore * rateWeight;

        index = Math.max(0.0, Math.min(1.0, index));
        return BigDecimal.valueOf(index).setScale(4, RoundingMode.HALF_UP);
    }

    private String getOpinionLevel(double index) {
        if (index >= 0.8) return "excellent";
        if (index >= 0.7) return "good";
        if (index >= 0.6) return "normal";
        if (index >= 0.4) return "attention";
        if (index >= 0.3) return "warning";
        return "critical";
    }

    private List<PublicOpinionVO.OpinionTrendVO> calculateTrendData(List<EventEvaluation> evaluations,
                                                                      LocalDate startDate, LocalDate endDate, Long gridId) {
        List<PublicOpinionVO.OpinionTrendVO> trend = new ArrayList<>();

        Map<String, List<EventEvaluation>> groupByDate = evaluations.stream()
                .filter(e -> e.getCreatedAt() != null)
                .collect(Collectors.groupingBy(e -> e.getCreatedAt().toLocalDate().format(DATE_FORMATTER)));

        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            String dateStr = date.format(DATE_FORMATTER);
            List<EventEvaluation> dayEvals = groupByDate.getOrDefault(dateStr, Collections.emptyList());

            PublicOpinionVO.OpinionTrendVO vo = new PublicOpinionVO.OpinionTrendVO();
            vo.setDate(dateStr);
            vo.setEvaluationCount(dayEvals.size());

            if (!dayEvals.isEmpty()) {
                double totalSentiment = 0;
                int count = 0;
                for (EventEvaluation eval : dayEvals) {
                    if (eval.getSentimentScore() != null) {
                        totalSentiment += eval.getSentimentScore();
                        count++;
                    }
                }
                double avgSentiment = count > 0 ? totalSentiment / count : 0.5;
                vo.setAvgSentimentScore(BigDecimal.valueOf(avgSentiment).setScale(4, RoundingMode.HALF_UP));

                double avgSpeed = dayEvals.stream()
                        .filter(e -> e.getSpeedScore() != null)
                        .mapToInt(EventEvaluation::getSpeedScore)
                        .average().orElse(3.0);
                double avgEffect = dayEvals.stream()
                        .filter(e -> e.getEffectScore() != null)
                        .mapToInt(EventEvaluation::getEffectScore)
                        .average().orElse(3.0);

                long positive = dayEvals.stream().filter(e -> "positive".equals(e.getSentimentLabel())).count();
                long negative = dayEvals.stream().filter(e -> "negative".equals(e.getSentimentLabel())).count();

                BigDecimal dayIndex = calculateOpinionIndex(avgSentiment, avgSpeed, avgEffect,
                        dayEvals.size(), (int) positive, (int) negative);
                vo.setOpinionIndex(dayIndex);
            } else {
                vo.setAvgSentimentScore(BigDecimal.valueOf(0.5));
                vo.setOpinionIndex(BigDecimal.valueOf(0.6));
            }

            trend.add(vo);
            date = date.plusDays(1);
        }

        return trend;
    }

    private List<PublicOpinionVO.WordCloudVO> calculateWordCloud(List<String> texts, int size) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<String, Integer>> keywords = wordCloudService.extractKeywords(texts, size);
        return keywords.stream()
                .map(entry -> {
                    PublicOpinionVO.WordCloudVO vo = new PublicOpinionVO.WordCloudVO();
                    vo.setName(entry.getKey());
                    vo.setValue(entry.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<PublicOpinionVO.HotEventVO> getHotNegativeEvents(String startDate, Long gridId, int limit) {
        List<EventEvaluation> evaluations = evaluationMapper.selectNegativeEvaluations(startDate, gridId, limit);

        List<PublicOpinionVO.HotEventVO> result = new ArrayList<>();
        for (EventEvaluation eval : evaluations) {
            PublicOpinionVO.HotEventVO vo = new PublicOpinionVO.HotEventVO();
            vo.setEventId(eval.getEventId());
            vo.setSentimentLabel(eval.getSentimentLabel());
            vo.setSentimentScore(eval.getSentimentScore());
            vo.setSpeedScore(eval.getSpeedScore());
            vo.setEffectScore(eval.getEffectScore());
            vo.setContent(eval.getContent());
            vo.setWarningLevel(eval.getWarningLevel());
            vo.setCreatedAt(eval.getCreatedAt() != null ? eval.getCreatedAt().format(DATE_TIME_FORMATTER) : null);

            EventInfo event = eventInfoMapper.selectById(eval.getEventId());
            if (event != null) {
                vo.setEventTitle(event.getTitle());
                vo.setEventType(event.getEventType());

                GridInfo grid = gridInfoMapper.selectById(event.getGridId());
                if (grid != null) {
                    vo.setGridName(grid.getGridName());
                }
            }

            result.add(vo);
        }
        return result;
    }

    private List<PublicOpinionVO.GridOpinionVO> getGridOpinions(List<EventEvaluation> evaluations) {
        Map<Long, List<EventEvaluation>> groupByGrid = new HashMap<>();

        for (EventEvaluation eval : evaluations) {
            EventInfo event = eventInfoMapper.selectById(eval.getEventId());
            if (event != null && event.getGridId() != null) {
                groupByGrid.computeIfAbsent(event.getGridId(), k -> new ArrayList<>()).add(eval);
            }
        }

        List<PublicOpinionVO.GridOpinionVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<EventEvaluation>> entry : groupByGrid.entrySet()) {
            Long gridId = entry.getKey();
            List<EventEvaluation> gridEvals = entry.getValue();

            PublicOpinionVO.GridOpinionVO vo = new PublicOpinionVO.GridOpinionVO();
            vo.setGridId(gridId);

            GridInfo grid = gridInfoMapper.selectById(gridId);
            if (grid != null) {
                vo.setGridName(grid.getGridName());
            }

            vo.setEvaluationCount(gridEvals.size());

            double avgSentiment = gridEvals.stream()
                    .filter(e -> e.getSentimentScore() != null)
                    .mapToDouble(EventEvaluation::getSentimentScore)
                    .average().orElse(0.5);

            double avgSpeed = gridEvals.stream()
                    .filter(e -> e.getSpeedScore() != null)
                    .mapToInt(EventEvaluation::getSpeedScore)
                    .average().orElse(3.0);

            double avgEffect = gridEvals.stream()
                    .filter(e -> e.getEffectScore() != null)
                    .mapToInt(EventEvaluation::getEffectScore)
                    .average().orElse(3.0);

            long positive = gridEvals.stream().filter(e -> "positive".equals(e.getSentimentLabel())).count();
            long negative = gridEvals.stream().filter(e -> "negative".equals(e.getSentimentLabel())).count();

            vo.setOpinionIndex(calculateOpinionIndex(avgSentiment, avgSpeed, avgEffect,
                    gridEvals.size(), (int) positive, (int) negative));

            int warningCount = (int) gridEvals.stream()
                    .filter(e -> "critical".equals(e.getWarningLevel()) || "warning".equals(e.getWarningLevel()))
                    .count();
            vo.setWarningCount(warningCount);

            result.add(vo);
        }

        result.sort((a, b) -> b.getOpinionIndex().compareTo(a.getOpinionIndex()));
        return result;
    }

    @Override
    public void calculateDailyStatistics() {
        log.info("开始计算每日舆情统计...");
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String dateStr = yesterday.format(DATE_FORMATTER);
        log.info("每日舆情统计计算完成: {}", dateStr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reprocessAllEvaluations() {
        log.info("开始重新处理所有评价的情感分析...");

        LambdaQueryWrapper<EventEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(EventEvaluation::getSentimentLabel)
                .or()
                .eq(EventEvaluation::getSentimentLabel, "");

        List<EventEvaluation> pending = evaluationMapper.selectList(wrapper);
        log.info("待处理评价数量: {}", pending.size());

        List<EventEvaluation> all = evaluationMapper.selectList(null);
        log.info("总评价数量: {}", all.size());

        int processed = 0;
        for (EventEvaluation eval : all) {
            if (StrUtil.isNotBlank(eval.getContent())) {
                SentimentAnalysisService.SentimentResult result = sentimentAnalysisService.analyze(eval.getContent());
                eval.setSentimentLabel(result.getLabel());
                eval.setSentimentScore(result.getScore());
                eval.setWarningLevel(result.getWarningLevel());

                List<Map.Entry<String, Integer>> keywords = wordCloudService.extractKeywords(eval.getContent(), 10);
                String keywordsStr = keywords.stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.joining(","));
                eval.setKeywords(keywordsStr);

                evaluationMapper.updateById(eval);
                processed++;
            }
        }

        log.info("情感分析重处理完成，共处理{}条", processed);
    }
}

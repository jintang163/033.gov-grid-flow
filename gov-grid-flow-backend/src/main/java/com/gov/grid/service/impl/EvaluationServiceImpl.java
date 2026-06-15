package com.gov.grid.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.EvaluationDTO;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.mapper.EventEvaluationMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.service.EvaluationService;
import com.gov.grid.service.SentimentAnalysisService;
import com.gov.grid.service.WordCloudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EventEvaluationMapper evaluationMapper;
    private final EventInfoMapper eventInfoMapper;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final WordCloudService wordCloudService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventEvaluation submitEvaluation(EvaluationDTO dto, Long userId) {
        EventInfo eventInfo = eventInfoMapper.selectById(dto.getEventId());
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        if (!EventStatus.COMPLETED.getCode().equals(eventInfo.getStatus())) {
            throw new BusinessException("事件尚未结案，无法评价");
        }

        if (eventInfo.getReporterId() != null && !eventInfo.getReporterId().equals(userId)) {
            throw new BusinessException("仅事件上报人可进行评价");
        }

        LambdaQueryWrapper<EventEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventEvaluation::getEventId, dto.getEventId());
        EventEvaluation existing = evaluationMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BusinessException("该事件已评价，请勿重复评价");
        }

        EventEvaluation evaluation = new EventEvaluation();
        evaluation.setEventId(dto.getEventId());
        evaluation.setReporterId(userId);
        evaluation.setSpeedScore(dto.getSpeedScore());
        evaluation.setEffectScore(dto.getEffectScore());
        evaluation.setContent(dto.getContent());

        evaluationMapper.insert(evaluation);
        log.info("评价提交成功，事件ID：{}，速度评分：{}，效果评分：{}", dto.getEventId(), dto.getSpeedScore(), dto.getEffectScore());

        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            try {
                SentimentAnalysisService.SentimentResult sentimentResult =
                        sentimentAnalysisService.analyze(dto.getContent());
                evaluation.setSentimentLabel(sentimentResult.getLabel());
                evaluation.setSentimentScore(sentimentResult.getScore());
                evaluation.setWarningLevel(sentimentResult.getWarningLevel());

                List<Map.Entry<String, Integer>> keywords =
                        wordCloudService.extractKeywords(dto.getContent(), 10);
                String keywordsStr = keywords.stream()
                        .map(Map.Entry::getKey)
                        .collect(Collectors.joining(","));
                evaluation.setKeywords(keywordsStr);

                evaluationMapper.updateById(evaluation);
                log.info("评价情感分析完成，事件ID：{}，情感：{}，分数：{}，预警：{}",
                        dto.getEventId(), sentimentResult.getLabel(),
                        sentimentResult.getScore(), sentimentResult.getWarningLevel());
            } catch (Exception e) {
                log.warn("评价情感分析失败，事件ID：{}，错误：{}", dto.getEventId(), e.getMessage());
            }
        }

        return evaluation;
    }

    @Override
    public EventEvaluation getEvaluationByEventId(Long eventId) {
        LambdaQueryWrapper<EventEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventEvaluation::getEventId, eventId);
        return evaluationMapper.selectOne(wrapper);
    }
}

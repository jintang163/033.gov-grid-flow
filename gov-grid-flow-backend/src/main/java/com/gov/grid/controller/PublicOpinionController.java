package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.dto.PublicOpinionQueryDTO;
import com.gov.grid.service.PublicOpinionService;
import com.gov.grid.service.SentimentAnalysisService;
import com.gov.grid.service.WordCloudService;
import com.gov.grid.vo.PublicOpinionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "舆情分析")
@RestController
@RequestMapping("/public-opinion")
public class PublicOpinionController {

    @Autowired
    private PublicOpinionService publicOpinionService;

    @Autowired
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private WordCloudService wordCloudService;

    @ApiOperation("获取舆情仪表盘数据")
    @PostMapping("/dashboard")
    public Result<PublicOpinionVO> getDashboard(@RequestBody PublicOpinionQueryDTO queryDTO) {
        PublicOpinionVO vo = publicOpinionService.getOpinionDashboard(queryDTO);
        return Result.success(vo);
    }

    @ApiOperation("获取指定网格的舆情数据")
    @PostMapping("/grid/{gridId}")
    public Result<PublicOpinionVO> getGridOpinion(@PathVariable Long gridId,
                                                   @RequestBody PublicOpinionQueryDTO queryDTO) {
        PublicOpinionVO vo = publicOpinionService.getGridOpinion(gridId, queryDTO);
        return Result.success(vo);
    }

    @ApiOperation("文本情感分析")
    @PostMapping("/sentiment/analyze")
    public Result<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        SentimentAnalysisService.SentimentResult result = sentimentAnalysisService.analyze(text);

        Map<String, Object> data = new HashMap<>();
        data.put("label", result.getLabel());
        data.put("score", result.getScore());
        data.put("warningLevel", result.getWarningLevel());
        data.put("isWarning", result.isWarning());
        return Result.success(data);
    }

    @ApiOperation("批量文本情感分析")
    @PostMapping("/sentiment/batch")
    public Result<List<Map<String, Object>>> batchAnalyze(@RequestBody List<String> texts) {
        List<Map<String, Object>> results = texts.stream().map(text -> {
            SentimentAnalysisService.SentimentResult result = sentimentAnalysisService.analyze(text);
            Map<String, Object> map = new HashMap<>();
            map.put("text", text);
            map.put("label", result.getLabel());
            map.put("score", result.getScore());
            map.put("warningLevel", result.getWarningLevel());
            return map;
        }).collect(Collectors.toList());
        return Result.success(results);
    }

    @ApiOperation("文本关键词提取（用于词云）")
    @PostMapping("/wordcloud")
    public Result<List<Map<String, Object>>> getWordCloud(@RequestBody Map<String, Object> body) {
        List<String> texts = (List<String>) body.get("texts");
        Integer topN = body.get("topN") != null ? ((Number) body.get("topN")).intValue() : 50;

        List<Map.Entry<String, Integer>> keywords = wordCloudService.extractKeywords(texts, topN);
        List<Map<String, Object>> result = keywords.stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", entry.getKey());
            map.put("value", entry.getValue());
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @ApiOperation("重新处理所有历史评价的情感分析")
    @PostMapping("/reprocess-all")
    public Result<Void> reprocessAllEvaluations() {
        publicOpinionService.reprocessAllEvaluations();
        return Result.success();
    }
}

package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.service.NlpDispatchService;
import com.gov.grid.vo.NlpDispatchResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "NLP智能分派")
@RestController
@RequestMapping("/nlp/dispatch")
@RequiredArgsConstructor
public class NlpDispatchController {

    private final NlpDispatchService nlpDispatchService;
    private final EventInfoMapper eventInfoMapper;

    @ApiOperation("智能分类推荐部门")
    @PostMapping("/classify")
    public Result<NlpDispatchResultVO> classify(@RequestBody Map<String, String> params) {
        String title = params.get("title");
        String description = params.get("description");
        String eventType = params.get("eventType");
        if (title == null || title.trim().isEmpty()) {
            return Result.error("事件标题不能为空");
        }
        NlpDispatchResultVO result = nlpDispatchService.classify(title, description, eventType);
        return Result.success("分类成功", result);
    }

    @ApiOperation("事件智能分派推荐（含保存记录）")
    @PostMapping("/recommend/{eventId}")
    public Result<NlpDispatchResultVO> recommend(@PathVariable Long eventId) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            return Result.error("事件不存在");
        }
        NlpDispatchResultVO result = nlpDispatchService.classifyWithEventId(
                eventId, eventInfo.getTitle(), eventInfo.getDescription(), eventInfo.getEventType()
        );
        return Result.success("推荐成功", result);
    }

    @ApiOperation("一键采纳分派推荐")
    @PostMapping("/adopt/{eventId}/{dispatchRecordId}")
    @PreAuthorize("hasRole('admin') or hasRole('grid_leader')")
    public Result<Void> adoptDispatch(
            @PathVariable Long eventId,
            @PathVariable Long dispatchRecordId,
            @RequestBody(required = false) Map<String, String> params) {
        String deptCode = params != null ? params.get("deptCode") : null;
        String deptName = params != null ? params.get("deptName") : null;
        boolean result = nlpDispatchService.adoptDispatch(eventId, dispatchRecordId, deptCode, deptName);
        return result ? Result.success("采纳成功", null) : Result.error("采纳失败");
    }

    @ApiOperation("拒绝分派推荐（修改部门）")
    @PostMapping("/reject/{eventId}/{dispatchRecordId}")
    @PreAuthorize("hasRole('admin') or hasRole('grid_leader')")
    public Result<Void> rejectDispatch(
            @PathVariable Long eventId,
            @PathVariable Long dispatchRecordId,
            @RequestBody Map<String, String> params) {
        String feedback = params.get("feedback");
        boolean result = nlpDispatchService.rejectDispatch(eventId, dispatchRecordId, feedback);
        return result ? Result.success("已拒绝推荐", null) : Result.error("操作失败");
    }

    @ApiOperation("获取事件分派历史")
    @GetMapping("/history/{eventId}")
    public Result<List<Map<String, Object>>> getDispatchHistory(@PathVariable Long eventId) {
        List<Map<String, Object>> history = nlpDispatchService.getDispatchHistory(eventId);
        return Result.success(history);
    }

    @ApiOperation("触发模型微调训练")
    @PostMapping("/train")
    @PreAuthorize("hasRole('admin')")
    public Result<Map<String, Object>> trainModel(@RequestBody Map<String, Object> params) {
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 1000;
        int epochs = params.get("epochs") != null ? Integer.parseInt(params.get("epochs").toString()) : 3;
        int batchSize = params.get("batchSize") != null ? Integer.parseInt(params.get("batchSize").toString()) : 16;
        double learningRate = params.get("learningRate") != null ? Double.parseDouble(params.get("learningRate").toString()) : 2e-5;

        List<Map<String, Object>> trainingData = nlpDispatchService.getTrainingData(limit);
        if (trainingData.isEmpty()) {
            return Result.error("暂无训练数据，请先积累分派记录");
        }

        Map<String, Object> trainResult = nlpDispatchService.triggerModelTraining(
                trainingData, epochs, batchSize, learningRate
        );
        return Result.success("训练完成", trainResult);
    }

    @ApiOperation("NLP服务健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        boolean available = nlpDispatchService.isNlpServiceAvailable();
        Map<String, Object> info = Map.of(
                "nlpServiceAvailable", available,
                "timestamp", java.time.LocalDateTime.now().toString()
        );
        return Result.success(info);
    }
}

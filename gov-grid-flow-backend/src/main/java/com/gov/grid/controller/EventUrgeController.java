package com.gov.grid.controller;

import com.gov.grid.annotation.AuditLog;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventUrgeRecord;
import com.gov.grid.entity.EventUrgeRule;
import com.gov.grid.entity.EventUrgeTemplate;
import com.gov.grid.service.EventService;
import com.gov.grid.service.EventUrgeService;
import com.gov.grid.vo.WarningInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "事件催办管理")
@RestController
@RequestMapping("/event/urge")
@RequiredArgsConstructor
public class EventUrgeController {

    private final EventUrgeService eventUrgeService;
    private final EventService eventService;

    @ApiOperation("催办规则列表")
    @GetMapping("/rule/list")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    public Result<List<EventUrgeRule>> listRules() {
        List<EventUrgeRule> list = eventUrgeService.listRules();
        return Result.success(list);
    }

    @ApiOperation("催办规则详情")
    @GetMapping("/rule/{id}")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    public Result<EventUrgeRule> getRuleById(@PathVariable Long id) {
        return Result.success(eventUrgeService.getRuleById(id));
    }

    @ApiOperation("新增催办规则")
    @PostMapping("/rule")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "create", description = "新增催办规则")
    public Result<EventUrgeRule> saveRule(@RequestBody EventUrgeRule rule) {
        EventUrgeRule saved = eventUrgeService.saveRule(rule);
        return Result.success("规则新增成功", saved);
    }

    @ApiOperation("修改催办规则")
    @PutMapping("/rule")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "update", description = "修改催办规则")
    public Result<EventUrgeRule> updateRule(@RequestBody EventUrgeRule rule) {
        EventUrgeRule updated = eventUrgeService.updateRule(rule);
        return Result.success("规则修改成功", updated);
    }

    @ApiOperation("删除催办规则")
    @DeleteMapping("/rule/{id}")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "delete", description = "删除催办规则")
    public Result<Boolean> deleteRule(@PathVariable Long id) {
        boolean deleted = eventUrgeService.deleteRule(id);
        return Result.success(deleted ? "删除成功" : "删除失败", deleted);
    }

    @ApiOperation("催办模板列表")
    @GetMapping("/template/list")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    public Result<List<EventUrgeTemplate>> listTemplates() {
        List<EventUrgeTemplate> list = eventUrgeService.listTemplates();
        return Result.success(list);
    }

    @ApiOperation("催办模板详情")
    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    public Result<EventUrgeTemplate> getTemplateById(@PathVariable Long id) {
        return Result.success(eventUrgeService.getTemplateById(id));
    }

    @ApiOperation("新增催办模板")
    @PostMapping("/template")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "create", description = "新增催办模板")
    public Result<EventUrgeTemplate> saveTemplate(@RequestBody EventUrgeTemplate template) {
        EventUrgeTemplate saved = eventUrgeService.saveTemplate(template);
        return Result.success("模板新增成功", saved);
    }

    @ApiOperation("修改催办模板")
    @PutMapping("/template")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "update", description = "修改催办模板")
    public Result<EventUrgeTemplate> updateTemplate(@RequestBody EventUrgeTemplate template) {
        EventUrgeTemplate updated = eventUrgeService.updateTemplate(template);
        return Result.success("模板修改成功", updated);
    }

    @ApiOperation("删除催办模板")
    @DeleteMapping("/template/{id}")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "delete", description = "删除催办模板")
    public Result<Boolean> deleteTemplate(@PathVariable Long id) {
        boolean deleted = eventUrgeService.deleteTemplate(id);
        return Result.success(deleted ? "删除成功" : "删除失败", deleted);
    }

    @ApiOperation("催办记录列表")
    @GetMapping("/record/list")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader')")
    public Result<PageResult<EventUrgeRecord>> listRecords(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<EventUrgeRecord> pageResult = eventUrgeService.listRecords(eventId, page, size);
        return Result.success(pageResult);
    }

    @ApiOperation("获取事件超时预警信息")
    @GetMapping("/warning/{eventId}")
    public Result<WarningInfoVO> getWarningInfo(@PathVariable Long eventId) {
        EventInfo event = eventService.getEventById(eventId);
        if (event == null) {
            return Result.error("事件不存在");
        }
        WarningInfoVO vo = eventUrgeService.getWarningInfo(event);
        return Result.success(vo);
    }

    @ApiOperation("手动执行催办扫描")
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    @AuditLog(module = "urge", operation = "execute", description = "手动执行催办扫描")
    public Result<Integer> scanAndUrge() {
        int count = eventUrgeService.scanAndUrge();
        return Result.success("扫描完成，处理了" + count + "个事件", count);
    }

    @ApiOperation("升级督办")
    @PostMapping("/escalate/{eventId}")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader')")
    @AuditLog(module = "urge", operation = "escalate", description = "升级督办")
    public Result<Boolean> escalateEvent(@PathVariable Long eventId) {
        EventInfo event = eventService.getEventById(eventId);
        if (event == null) {
            return Result.error("事件不存在");
        }
        boolean success = eventUrgeService.escalateEvent(event, "SUPERIOR");
        return Result.success(success ? "升级成功" : "升级失败", success);
    }
}

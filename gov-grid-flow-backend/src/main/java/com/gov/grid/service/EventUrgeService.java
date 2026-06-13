package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventUrgeRecord;
import com.gov.grid.entity.EventUrgeRule;
import com.gov.grid.entity.EventUrgeTemplate;
import com.gov.grid.vo.WarningInfoVO;

import java.time.LocalDateTime;
import java.util.List;

public interface EventUrgeService {

    List<EventUrgeRule> listRules();

    EventUrgeRule getRuleById(Long id);

    EventUrgeRule getRuleByEventType(String eventType);

    EventUrgeRule saveRule(EventUrgeRule rule);

    EventUrgeRule updateRule(EventUrgeRule rule);

    boolean deleteRule(Long id);

    List<EventUrgeTemplate> listTemplates();

    EventUrgeTemplate getTemplateById(Long id);

    EventUrgeTemplate getTemplateByCode(String templateCode);

    EventUrgeTemplate saveTemplate(EventUrgeTemplate template);

    EventUrgeTemplate updateTemplate(EventUrgeTemplate template);

    boolean deleteTemplate(Long id);

    PageResult<EventUrgeRecord> listRecords(Long eventId, Integer page, Integer size);

    EventUrgeRecord createRecord(EventUrgeRecord record);

    LocalDateTime calculateDeadline(String eventType, LocalDateTime startTime);

    WarningInfoVO getWarningInfo(EventInfo event);

    int scanAndUrge();

    boolean escalateEvent(EventInfo event, String escalateLevel);
}

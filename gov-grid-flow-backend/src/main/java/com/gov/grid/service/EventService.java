package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.dto.EventQueryDTO;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.vo.EventDetailVO;

public interface EventService {

    EventInfo reportEvent(EventReportDTO dto, Long userId);

    PageResult<EventInfo> getEventList(EventQueryDTO dto);

    EventDetailVO getEventDetail(Long eventId);

    void updateEventStatus(Long eventId, String status);

    String generateEventNo();
}

package com.gov.grid.vo;

import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventProcess;
import lombok.Data;

import java.util.List;

@Data
public class EventDetailVO {

    private EventInfo eventInfo;

    private List<EventProcess> processList;

    private EventEvaluation evaluation;
}

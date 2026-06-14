package com.gov.grid.vo;

import com.gov.grid.dto.ImageComparisonResultVO;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventProcess;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EventDetailVO {

    private EventInfo eventInfo;

    private List<EventProcess> processList;

    private EventEvaluation evaluation;

    private List<ImageComparisonResultVO> comparisonList;

    private NlpDispatchResultVO dispatchRecommendation;

    private List<Map<String, Object>> dispatchHistory;
}


package com.gov.grid.service;

import com.gov.grid.dto.EvaluationDTO;
import com.gov.grid.entity.EventEvaluation;

public interface EvaluationService {

    EventEvaluation submitEvaluation(EvaluationDTO dto, Long userId);

    EventEvaluation getEvaluationByEventId(Long eventId);
}

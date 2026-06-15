package com.gov.grid.service;

import com.gov.grid.dto.PublicOpinionQueryDTO;
import com.gov.grid.vo.PublicOpinionVO;

public interface PublicOpinionService {

    PublicOpinionVO getOpinionDashboard(PublicOpinionQueryDTO queryDTO);

    PublicOpinionVO getGridOpinion(Long gridId, PublicOpinionQueryDTO queryDTO);

    void calculateDailyStatistics();

    void reprocessAllEvaluations();
}

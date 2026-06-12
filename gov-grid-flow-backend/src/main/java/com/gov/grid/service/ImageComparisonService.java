package com.gov.grid.service;

import com.gov.grid.dto.ImageComparisonDTO;
import com.gov.grid.dto.ImageComparisonResultVO;

import java.util.List;

public interface ImageComparisonService {

    ImageComparisonResultVO compareImages(ImageComparisonDTO dto);

    ImageComparisonResultVO compareAndSave(ImageComparisonDTO dto, Long userId);

    List<ImageComparisonResultVO> getComparisonHistory(Long eventId);
}

package com.gov.grid.service;

import com.gov.grid.vo.LocationReportDTO;
import com.gov.grid.vo.NearbyResourceVO;

import java.math.BigDecimal;

public interface NearbyResourceService {

    NearbyResourceVO getNearbyResources(BigDecimal lng, BigDecimal lat, Integer radius);

    void reportLocation(Long userId, LocationReportDTO dto);
}

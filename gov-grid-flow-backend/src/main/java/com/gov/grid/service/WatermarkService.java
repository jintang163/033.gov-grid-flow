package com.gov.grid.service;

import com.gov.grid.dto.WatermarkDTO;
import com.gov.grid.vo.TamperCheckVO;
import com.gov.grid.vo.WatermarkResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface WatermarkService {

    WatermarkResultVO addWatermark(WatermarkDTO watermarkDTO) throws IOException;

    TamperCheckVO checkTamper(String fileUrl) throws IOException;

    List<TamperCheckVO> checkEventFilesTamper(Long eventId) throws IOException;

    WatermarkResultVO uploadWithWatermark(MultipartFile file, String reportTime,
                                           String reporterName, String eventNo,
                                           Long eventId, Long reporterId,
                                           Boolean sensitive) throws IOException;
}

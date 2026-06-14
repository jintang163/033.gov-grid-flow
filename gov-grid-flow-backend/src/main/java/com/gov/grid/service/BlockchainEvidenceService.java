package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.vo.BlockchainEvidenceVO;

import java.util.List;
import java.util.Map;

public interface BlockchainEvidenceService {

    BlockchainEvidenceVO createEvidence(Long eventId);

    BlockchainEvidenceVO getEvidenceByEventId(Long eventId);

    BlockchainEvidenceVO getEvidenceById(Long evidenceId);

    List<BlockchainEvidenceVO> getEvidenceList(Long eventId);

    PageResult<BlockchainEvidenceVO> getEvidencePage(int pageNum, int pageSize, String evidenceNo, String keyword,
                                                      String status, Integer verified, String startTime, String endTime);

    Map<String, Object> verifyEvidence(Long evidenceId);

    String generateCertificateQrCode(Long evidenceId);

    boolean isHighRiskEventType(String eventType);
}

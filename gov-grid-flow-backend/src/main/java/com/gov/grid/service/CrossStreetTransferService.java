package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.dto.CrossStreetTransferApproveDTO;
import com.gov.grid.dto.CrossStreetTransferDTO;
import com.gov.grid.dto.CrossStreetTransferProcessDTO;
import com.gov.grid.vo.CrossStreetTransferVO;
import com.gov.grid.vo.DeptTreeVO;
import com.gov.grid.vo.TransferTraceVO;

import java.util.List;
import java.util.Map;

public interface CrossStreetTransferService {

    Long applyTransfer(CrossStreetTransferDTO dto, Long userId);

    boolean approveTransfer(CrossStreetTransferApproveDTO dto, Long userId);

    boolean receiveTransfer(Long transferId, Long userId);

    boolean processTransfer(CrossStreetTransferProcessDTO dto, Long userId);

    boolean completeTransfer(CrossStreetTransferProcessDTO dto, Long userId);

    CrossStreetTransferVO getTransferDetail(Long transferId);

    PageResult<CrossStreetTransferVO> getTransferPage(
            Integer pageNum, Integer pageSize,
            Long eventId, Long sourceDeptId, Long targetDeptId,
            String status, String targetType, Long userId, String keyword
    );

    List<CrossStreetTransferVO> getMyInvolvedTransfers(Long userId, Long deptId, String status);

    List<CrossStreetTransferVO> getEventTransferHistory(Long eventId);

    List<TransferTraceVO> getTransferTrace(Long transferId);

    List<DeptTreeVO> getCooperationDeptTree(String targetType, Long sourceDeptId);

    Map<String, Object> getTransferStatistics(Long deptId);

    boolean isCrossStreetEvent(Long eventId, Long currentDeptId);

    List<Map<String, Object>> getRecommendedTargets(Long eventId, String targetType);
}

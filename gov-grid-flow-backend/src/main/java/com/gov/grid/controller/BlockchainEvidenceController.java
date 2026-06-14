package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.service.BlockchainEvidenceService;
import com.gov.grid.vo.BlockchainEvidenceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "区块链存证")
@RestController
@RequestMapping("/blockchain/evidence")
@RequiredArgsConstructor
public class BlockchainEvidenceController {

    private final BlockchainEvidenceService blockchainEvidenceService;

    @ApiOperation("创建区块链存证")
    @PostMapping("/create/{eventId}")
    @PreAuthorize("hasRole('admin') or hasRole('grid_leader') or hasRole('street_manager')")
    public Result<BlockchainEvidenceVO> createEvidence(@PathVariable Long eventId) {
        BlockchainEvidenceVO evidence = blockchainEvidenceService.createEvidence(eventId);
        return Result.success("存证创建成功", evidence);
    }

    @ApiOperation("获取事件存证信息")
    @GetMapping("/event/{eventId}")
    public Result<BlockchainEvidenceVO> getEvidenceByEventId(@PathVariable Long eventId) {
        BlockchainEvidenceVO evidence = blockchainEvidenceService.getEvidenceByEventId(eventId);
        if (evidence == null) {
            return Result.error("暂无存证记录");
        }
        return Result.success(evidence);
    }

    @ApiOperation("获取存证详情")
    @GetMapping("/{evidenceId}")
    public Result<BlockchainEvidenceVO> getEvidenceById(@PathVariable Long evidenceId) {
        BlockchainEvidenceVO evidence = blockchainEvidenceService.getEvidenceById(evidenceId);
        return Result.success(evidence);
    }

    @ApiOperation("获取事件存证列表")
    @GetMapping("/list/event/{eventId}")
    public Result<List<BlockchainEvidenceVO>> getEvidenceList(@PathVariable Long eventId) {
        List<BlockchainEvidenceVO> list = blockchainEvidenceService.getEvidenceList(eventId);
        return Result.success(list);
    }

    @ApiOperation("存证列表（分页）")
    @GetMapping("/list")
    @PreAuthorize("hasRole('admin') or hasRole('street_manager') or hasRole('grid_leader') or hasRole('supervisor')")
    public Result<PageResult<BlockchainEvidenceVO>> getEvidencePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String evidenceNo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer verified,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        PageResult<BlockchainEvidenceVO> result = blockchainEvidenceService.getEvidencePage(
                pageNum, pageSize, evidenceNo, keyword, status, verified, startTime, endTime);
        return Result.success(result);
    }

    @ApiOperation("核验存证")
    @PostMapping("/verify/{evidenceId}")
    public Result<Map<String, Object>> verifyEvidence(@PathVariable Long evidenceId) {
        Map<String, Object> result = blockchainEvidenceService.verifyEvidence(evidenceId);
        boolean valid = result.get("valid") != null && (Boolean) result.get("valid");
        return Result.success(valid ? "存证核验通过" : "存证核验失败", result);
    }

    @ApiOperation("获取存证证书二维码")
    @GetMapping("/qrcode/{evidenceId}")
    public Result<String> getQrCode(@PathVariable Long evidenceId) {
        String qrCodeData = blockchainEvidenceService.generateCertificateQrCode(evidenceId);
        return Result.success(qrCodeData);
    }

    @ApiOperation("判断是否为高风险事件类型")
    @GetMapping("/high-risk/{eventType}")
    public Result<Boolean> isHighRiskType(@PathVariable String eventType) {
        boolean result = blockchainEvidenceService.isHighRiskEventType(eventType);
        return Result.success(result);
    }
}

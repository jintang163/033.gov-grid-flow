package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.WatermarkService;
import com.gov.grid.vo.TamperCheckVO;
import com.gov.grid.vo.WatermarkResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Api(tags = "水印管理")
@RestController
@RequestMapping("/watermark")
@RequiredArgsConstructor
public class WatermarkController {

    private final WatermarkService watermarkService;

    @ApiOperation("上传图片并添加水印")
    @PostMapping("/upload")
    public Result<WatermarkResultVO> uploadWithWatermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam("reportTime") String reportTime,
            @RequestParam("reporterName") String reporterName,
            @RequestParam(value = "eventNo", required = false) String eventNo,
            @RequestParam(value = "eventId", required = false) Long eventId,
            @RequestParam(value = "reporterId", required = false) Long reporterId,
            @RequestParam(value = "sensitive", required = false, defaultValue = "false") Boolean sensitive) throws IOException {

        WatermarkResultVO result = watermarkService.uploadWithWatermark(
                file, reportTime, reporterName, eventNo, eventId, reporterId, sensitive
        );
        return Result.success("水印上传成功", result);
    }

    @ApiOperation("批量上传图片并添加水印")
    @PostMapping("/upload/batch")
    public Result<List<WatermarkResultVO>> batchUploadWithWatermark(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("reportTime") String reportTime,
            @RequestParam("reporterName") String reporterName,
            @RequestParam(value = "eventNo", required = false) String eventNo,
            @RequestParam(value = "eventId", required = false) Long eventId,
            @RequestParam(value = "reporterId", required = false) Long reporterId,
            @RequestParam(value = "sensitive", required = false, defaultValue = "false") Boolean sensitive) throws IOException {

        List<WatermarkResultVO> results = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            WatermarkResultVO result = watermarkService.uploadWithWatermark(
                    file, reportTime, reporterName, eventNo, eventId, reporterId, sensitive
            );
            results.add(result);
        }
        return Result.success("批量水印上传成功", results);
    }

    @ApiOperation("检测文件是否被篡改")
    @GetMapping("/check-tamper")
    public Result<TamperCheckVO> checkTamper(@RequestParam("fileUrl") String fileUrl) throws IOException {
        TamperCheckVO result = watermarkService.checkTamper(fileUrl);
        return Result.success(result.getMessage(), result);
    }

    @ApiOperation("检测事件所有附件是否被篡改")
    @GetMapping("/check-event-tamper/{eventId}")
    public Result<List<TamperCheckVO>> checkEventFilesTamper(@PathVariable Long eventId) throws IOException {
        List<TamperCheckVO> results = watermarkService.checkEventFilesTamper(eventId);
        return Result.success("检测完成", results);
    }
}

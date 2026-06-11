package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.NearbyResourceService;
import com.gov.grid.vo.LocationReportDTO;
import com.gov.grid.vo.NearbyResourceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;

@Slf4j
@Api(tags = "周边资源")
@RestController
@RequestMapping("/nearby")
@RequiredArgsConstructor
public class NearbyResourceController {

    private final NearbyResourceService nearbyResourceService;

    @ApiOperation("获取周边资源")
    @GetMapping("/resources")
    public Result<NearbyResourceVO> getNearbyResources(
            @ApiParam(value = "经度", required = true)
            @RequestParam BigDecimal lng,
            @ApiParam(value = "纬度", required = true)
            @RequestParam BigDecimal lat,
            @ApiParam(value = "搜索半径(米)，默认500")
            @RequestParam(required = false, defaultValue = "500") Integer radius) {
        NearbyResourceVO vo = nearbyResourceService.getNearbyResources(lng, lat, radius);
        return Result.success(vo);
    }

    @ApiOperation("网格员上报位置")
    @PostMapping("/member/location/report")
    public Result<Void> reportLocation(
            @ApiParam(hidden = true)
            @RequestHeader(value = "X-User-Id", required = false) String userIdStr,
            @Valid @RequestBody LocationReportDTO dto) {
        Long userId = null;
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                log.warn("X-User-Id格式错误: {}", userIdStr);
            }
        }
        nearbyResourceService.reportLocation(userId, dto);
        return Result.success();
    }
}

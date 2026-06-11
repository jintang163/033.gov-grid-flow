package com.gov.grid.service.impl;

import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.GridMemberLocation;
import com.gov.grid.entity.ResourceCamera;
import com.gov.grid.entity.ResourceEmergency;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.GridMemberLocationMapper;
import com.gov.grid.mapper.ResourceCameraMapper;
import com.gov.grid.mapper.ResourceEmergencyMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.NearbyResourceService;
import com.gov.grid.vo.CameraVO;
import com.gov.grid.vo.EmergencyVO;
import com.gov.grid.vo.LocationReportDTO;
import com.gov.grid.vo.MemberLocationVO;
import com.gov.grid.vo.NearbyResourceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NearbyResourceServiceImpl implements NearbyResourceService {

    private final ResourceCameraMapper resourceCameraMapper;
    private final ResourceEmergencyMapper resourceEmergencyMapper;
    private final GridMemberLocationMapper gridMemberLocationMapper;
    private final SysUserMapper sysUserMapper;
    private final GridInfoMapper gridInfoMapper;

    private static final Map<String, String> CAMERA_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> RESOURCE_TYPE_MAP = new HashMap<>();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        CAMERA_TYPE_MAP.put("public", "公共治安");
        CAMERA_TYPE_MAP.put("traffic", "交通监控");
        CAMERA_TYPE_MAP.put("private", "小区监控");

        RESOURCE_TYPE_MAP.put("fire_fighting", "消防物资");
        RESOURCE_TYPE_MAP.put("first_aid", "急救物资");
        RESOURCE_TYPE_MAP.put("flood_ctr", "防汛物资");
        RESOURCE_TYPE_MAP.put("medical", "医疗物资");
        RESOURCE_TYPE_MAP.put("rescue", "救援物资");
    }

    @Override
    public NearbyResourceVO getNearbyResources(BigDecimal lng, BigDecimal lat, Integer radius) {
        radius = radius != null ? radius : 500;

        List<ResourceCamera> cameras = resourceCameraMapper.findNearby(lng, lat, radius);
        List<ResourceEmergency> emergencies = resourceEmergencyMapper.findNearby(lng, lat, radius);
        List<GridMemberLocation> members = gridMemberLocationMapper.findNearby(lng, lat, radius);

        List<CameraVO> cameraVOList = cameras.stream().map(camera -> {
            CameraVO vo = new CameraVO();
            vo.setId(camera.getId());
            vo.setCameraCode(camera.getCameraCode());
            vo.setCameraName(camera.getCameraName());
            vo.setCameraType(camera.getCameraType());
            vo.setCameraTypeName(CAMERA_TYPE_MAP.getOrDefault(camera.getCameraType(), camera.getCameraType()));
            vo.setLng(camera.getLng());
            vo.setLat(camera.getLat());
            vo.setAddress(camera.getAddress());
            vo.setHlsUrl(camera.getHlsUrl());
            vo.setStatus(camera.getStatus());
            vo.setDistance(calculateDistance(lng, lat, camera.getLng(), camera.getLat()));
            return vo;
        }).collect(Collectors.toList());

        List<EmergencyVO> emergencyVOList = emergencies.stream().map(emergency -> {
            EmergencyVO vo = new EmergencyVO();
            vo.setId(emergency.getId());
            vo.setResourceCode(emergency.getResourceCode());
            vo.setResourceName(emergency.getResourceName());
            vo.setResourceType(emergency.getResourceType());
            vo.setResourceTypeName(RESOURCE_TYPE_MAP.getOrDefault(emergency.getResourceType(), emergency.getResourceType()));
            vo.setQuantity(emergency.getQuantity());
            vo.setLng(emergency.getLng());
            vo.setLat(emergency.getLat());
            vo.setAddress(emergency.getAddress());
            vo.setManager(emergency.getManager());
            vo.setManagerPhone(emergency.getManagerPhone());
            vo.setStatus(emergency.getStatus());
            vo.setDistance(calculateDistance(lng, lat, emergency.getLng(), emergency.getLat()));
            return vo;
        }).collect(Collectors.toList());

        List<MemberLocationVO> memberVOList = members.stream().map(member -> {
            MemberLocationVO vo = new MemberLocationVO();
            vo.setUserId(member.getUserId());
            vo.setUserName(member.getUserName());
            vo.setPhone(member.getPhone());
            vo.setGridId(member.getGridId());
            if (member.getGridId() != null) {
                GridInfo grid = gridInfoMapper.selectById(member.getGridId());
                if (grid != null) {
                    vo.setGridName(grid.getGridName());
                }
            }
            vo.setLng(member.getLng());
            vo.setLat(member.getLat());
            vo.setAddress(member.getAddress());
            vo.setOnDuty(member.getOnDuty());
            vo.setLastReportTime(member.getLastReportTime() != null
                    ? member.getLastReportTime().format(DATE_TIME_FORMATTER) : null);
            vo.setDistance(calculateDistance(lng, lat, member.getLng(), member.getLat()));
            vo.setBattery(member.getBattery());
            return vo;
        }).collect(Collectors.toList());

        NearbyResourceVO result = new NearbyResourceVO();
        result.setRadius(radius);
        result.setCameraCount(cameraVOList.size());
        result.setEmergencyCount(emergencyVOList.size());
        result.setMemberCount(memberVOList.size());
        result.setCameras(cameraVOList);
        result.setEmergencies(emergencyVOList);
        result.setMembers(memberVOList);

        log.info("查询周边资源完成，半径:{}米，摄像头:{}个，应急物资:{}个，网格员:{}个",
                radius, cameraVOList.size(), emergencyVOList.size(), memberVOList.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportLocation(Long userId, LocationReportDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            log.warn("上报位置失败，用户不存在: userId={}", userId);
            return;
        }

        GridMemberLocation location = new GridMemberLocation();
        location.setUserId(userId);
        location.setUserName(user.getRealName());
        location.setPhone(user.getPhone());
        location.setGridId(user.getGridId());
        location.setLng(dto.getLng());
        location.setLat(dto.getLat());
        location.setAddress(dto.getAddress());
        location.setOnDuty(dto.getOnDuty() != null ? dto.getOnDuty() : 1);
        location.setAccuracy(dto.getAccuracy());
        location.setBattery(dto.getBattery());
        location.setLastReportTime(LocalDateTime.now());

        gridMemberLocationMapper.upsertLocation(location);
        log.info("网格员上报位置成功，userId:{}, 姓名:{}, 位置:[{},{}]",
                userId, user.getRealName(), dto.getLng(), dto.getLat());
    }

    private double calculateDistance(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }
}

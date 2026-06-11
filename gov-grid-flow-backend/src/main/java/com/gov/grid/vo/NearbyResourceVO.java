package com.gov.grid.vo;

import lombok.Data;

import java.util.List;

@Data
public class NearbyResourceVO {
    private Integer radius;
    private Integer cameraCount;
    private Integer emergencyCount;
    private Integer memberCount;
    private List<CameraVO> cameras;
    private List<EmergencyVO> emergencies;
    private List<MemberLocationVO> members;
}

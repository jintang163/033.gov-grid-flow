package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_camera")
public class ResourceCamera extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("camera_code")
    private String cameraCode;

    @TableField("camera_name")
    private String cameraName;

    @TableField("camera_type")
    private String cameraType;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("address")
    private String address;

    @TableField("rtsp_url")
    private String rtspUrl;

    @TableField("hls_url")
    private String hlsUrl;

    @TableField("grid_id")
    private Long gridId;

    @TableField("manufacturer")
    private String manufacturer;

    @TableField("status")
    private Integer status;
}

package com.gov.grid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.grid.entity.ResourceEmergency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ResourceEmergencyMapper extends BaseMapper<ResourceEmergency> {

    @Select("SELECT * FROM resource_emergency WHERE deleted=0 AND " +
            "ST_Distance_Sphere(POINT(#{lng}, #{lat}), POINT(lng, lat)) <= #{radius} " +
            "ORDER BY ST_Distance_Sphere(POINT(#{lng}, #{lat}), POINT(lng, lat)) ASC")
    List<ResourceEmergency> findNearby(@Param("lng") BigDecimal lng, @Param("lat") BigDecimal lat, @Param("radius") Integer radius);
}

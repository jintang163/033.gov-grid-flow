package com.gov.grid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.grid.entity.GridMemberLocation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface GridMemberLocationMapper extends BaseMapper<GridMemberLocation> {

    @Select("SELECT * FROM grid_member_location WHERE on_duty=1 AND " +
            "ST_Distance_Sphere(POINT(#{lng}, #{lat}), POINT(lng, lat)) <= #{radius} " +
            "ORDER BY ST_Distance_Sphere(POINT(#{lng}, #{lat}), POINT(lng, lat)) ASC")
    List<GridMemberLocation> findNearby(@Param("lng") BigDecimal lng, @Param("lat") BigDecimal lat, @Param("radius") Integer radius);

    @Insert("INSERT INTO grid_member_location (user_id, user_name, phone, grid_id, lng, lat, address, " +
            "on_duty, last_report_time, accuracy, battery) VALUES (#{userId}, #{userName}, #{phone}, #{gridId}, " +
            "#{lng}, #{lat}, #{address}, #{onDuty}, #{lastReportTime}, #{accuracy}, #{battery}) " +
            "ON DUPLICATE KEY UPDATE user_name=VALUES(user_name), phone=VALUES(phone), grid_id=VALUES(grid_id), " +
            "lng=VALUES(lng), lat=VALUES(lat), address=VALUES(address), on_duty=VALUES(on_duty), " +
            "last_report_time=VALUES(last_report_time), accuracy=VALUES(accuracy), battery=VALUES(battery)")
    int upsertLocation(GridMemberLocation location);
}

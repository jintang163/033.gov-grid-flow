package com.gov.grid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.grid.entity.PublicOpinionDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PublicOpinionDailyMapper extends BaseMapper<PublicOpinionDaily> {

    @Select("SELECT * FROM public_opinion_daily " +
            "WHERE stat_date BETWEEN #{startDate} AND #{endDate} " +
            "AND (#{gridId} IS NULL OR grid_id = #{gridId}) " +
            "AND deleted = 0 " +
            "ORDER BY stat_date ASC")
    List<PublicOpinionDaily> selectByDateRangeAndGrid(@Param("startDate") String startDate,
                                                       @Param("endDate") String endDate,
                                                       @Param("gridId") Long gridId);

    @Select("SELECT * FROM public_opinion_daily " +
            "WHERE stat_date = #{statDate} " +
            "AND grid_id = #{gridId} " +
            "AND deleted = 0 " +
            "LIMIT 1")
    PublicOpinionDaily selectByDateAndGrid(@Param("statDate") String statDate,
                                            @Param("gridId") Long gridId);

    @Select("SELECT pod.* FROM public_opinion_daily pod " +
            "INNER JOIN ( " +
            "  SELECT grid_id, MAX(stat_date) AS latest_date " +
            "  FROM public_opinion_daily " +
            "  WHERE stat_date BETWEEN #{startDate} AND #{endDate} " +
            "  AND deleted = 0 " +
            "  GROUP BY grid_id " +
            ") latest ON pod.grid_id = latest.grid_id AND pod.stat_date = latest.latest_date " +
            "WHERE pod.deleted = 0 " +
            "ORDER BY pod.opinion_index ASC")
    List<PublicOpinionDaily> selectGridRanking(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate);
}

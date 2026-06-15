package com.gov.grid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.grid.entity.EventEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EventEvaluationMapper extends BaseMapper<EventEvaluation> {

    @Select("SELECT e.* FROM event_evaluation e " +
            "INNER JOIN event_info ei ON e.event_id = ei.id " +
            "WHERE e.created_at BETWEEN #{startDate} AND #{endDate} " +
            "AND (#{gridId} IS NULL OR ei.grid_id = #{gridId}) " +
            "ORDER BY e.created_at DESC")
    List<EventEvaluation> selectByDateRangeAndGrid(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate,
                                                    @Param("gridId") Long gridId);

    @Select("SELECT e.* FROM event_evaluation e " +
            "INNER JOIN event_info ei ON e.event_id = ei.id " +
            "WHERE e.sentiment_label = 'negative' " +
            "AND e.created_at >= #{startDate} " +
            "AND (#{gridId} IS NULL OR ei.grid_id = #{gridId}) " +
            "ORDER BY e.sentiment_score ASC, e.created_at DESC " +
            "LIMIT #{limit}")
    List<EventEvaluation> selectNegativeEvaluations(@Param("startDate") String startDate,
                                                      @Param("gridId") Long gridId,
                                                      @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM event_evaluation e " +
            "INNER JOIN event_info ei ON e.event_id = ei.id " +
            "WHERE e.created_at BETWEEN #{startDate} AND #{endDate} " +
            "AND (#{gridId} IS NULL OR ei.grid_id = #{gridId}) " +
            "AND e.sentiment_label = #{label}")
    Integer countBySentimentLabel(@Param("startDate") String startDate,
                                   @Param("endDate") String endDate,
                                   @Param("gridId") Long gridId,
                                   @Param("label") String label);
}

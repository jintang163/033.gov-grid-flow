package com.gov.grid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.grid.entity.EventCrossStreetTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EventCrossStreetTransferMapper extends BaseMapper<EventCrossStreetTransfer> {

    List<EventCrossStreetTransfer> selectTransferList(
            @Param("eventId") Long eventId,
            @Param("sourceDeptId") Long sourceDeptId,
            @Param("targetDeptId") Long targetDeptId,
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("userId") Long userId,
            @Param("keyword") String keyword
    );

    List<EventCrossStreetTransfer> selectMyInvolvedTransfers(
            @Param("userId") Long userId,
            @Param("deptId") Long deptId,
            @Param("status") String status
    );
}

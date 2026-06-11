package com.gov.grid.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.grid.entity.GridMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GridMemberMapper extends BaseMapper<GridMember> {
}

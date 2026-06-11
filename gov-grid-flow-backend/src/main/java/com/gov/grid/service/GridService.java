package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysUser;

import java.util.List;

public interface GridService {

    GridInfo create(GridInfo gridInfo);

    GridInfo update(GridInfo gridInfo);

    void delete(Long id);

    GridInfo getById(Long id);

    List<GridInfo> listAll();

    PageResult<GridInfo> page(Integer pageNum, Integer pageSize, String keyword);

    List<SysUser> getMembers(Long gridId);
}

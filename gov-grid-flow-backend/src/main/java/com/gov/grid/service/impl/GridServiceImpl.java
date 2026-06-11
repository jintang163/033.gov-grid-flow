package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.GridMember;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.GridMemberMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.GridService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridServiceImpl implements GridService {

    private final GridInfoMapper gridInfoMapper;
    private final GridMemberMapper gridMemberMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GridInfo create(GridInfo gridInfo) {
        if (StrUtil.isBlank(gridInfo.getGridCode())) {
            throw new BusinessException("网格编码不能为空");
        }
        if (StrUtil.isBlank(gridInfo.getGridName())) {
            throw new BusinessException("网格名称不能为空");
        }

        LambdaQueryWrapper<GridInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridInfo::getGridCode, gridInfo.getGridCode());
        Long count = gridInfoMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("网格编码已存在");
        }

        gridInfoMapper.insert(gridInfo);
        log.info("创建网格成功，网格ID：{}，网格名称：{}", gridInfo.getId(), gridInfo.getGridName());
        return gridInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GridInfo update(GridInfo gridInfo) {
        GridInfo exist = gridInfoMapper.selectById(gridInfo.getId());
        if (exist == null) {
            throw new BusinessException("网格不存在");
        }
        gridInfoMapper.updateById(gridInfo);
        log.info("更新网格成功，网格ID：{}", gridInfo.getId());
        return gridInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GridInfo exist = gridInfoMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("网格不存在");
        }
        gridInfoMapper.deleteById(id);

        LambdaQueryWrapper<GridMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GridMember::getGridId, id);
        gridMemberMapper.delete(memberWrapper);

        log.info("删除网格成功，网格ID：{}", id);
    }

    @Override
    public GridInfo getById(Long id) {
        GridInfo gridInfo = gridInfoMapper.selectById(id);
        if (gridInfo == null) {
            throw new BusinessException("网格不存在");
        }
        return gridInfo;
    }

    @Override
    public List<GridInfo> listAll() {
        LambdaQueryWrapper<GridInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridInfo::getStatus, 1);
        wrapper.orderByAsc(GridInfo::getGridCode);
        return gridInfoMapper.selectList(wrapper);
    }

    @Override
    public PageResult<GridInfo> page(Integer pageNum, Integer pageSize, String keyword) {
        pageNum = pageNum != null ? pageNum : 1;
        pageSize = pageSize != null ? pageSize : 10;

        Page<GridInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<GridInfo> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(GridInfo::getGridName, keyword)
                    .or().like(GridInfo::getGridCode, keyword)
                    .or().like(GridInfo::getAddress, keyword));
        }

        wrapper.orderByDesc(GridInfo::getCreatedAt);
        Page<GridInfo> result = gridInfoMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), pageNum, pageSize);
    }

    @Override
    public List<SysUser> getMembers(Long gridId) {
        LambdaQueryWrapper<GridMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(GridMember::getGridId, gridId);
        List<GridMember> members = gridMemberMapper.selectList(memberWrapper);

        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = members.stream().map(GridMember::getUserId).collect(Collectors.toList());
        return sysUserMapper.selectBatchIds(userIds);
    }
}

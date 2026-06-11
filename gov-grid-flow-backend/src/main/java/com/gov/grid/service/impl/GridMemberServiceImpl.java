package com.gov.grid.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.GridMember;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.GridMemberMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.GridMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GridMemberServiceImpl implements GridMemberService {

    private final GridMemberMapper gridMemberMapper;
    private final GridInfoMapper gridInfoMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long gridId, Long userId, String memberType) {
        validateGridAndUser(gridId, userId);

        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getGridId, gridId).eq(GridMember::getUserId, userId);
        Long count = gridMemberMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该用户已在网格中");
        }

        GridMember member = new GridMember();
        member.setGridId(gridId);
        member.setUserId(userId);
        member.setMemberType(memberType != null ? memberType : "MEMBER");
        gridMemberMapper.insert(member);

        log.info("添加网格员成功，网格ID：{}，用户ID：{}，类型：{}", gridId, userId, memberType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long gridId, Long userId) {
        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getGridId, gridId).eq(GridMember::getUserId, userId);
        gridMemberMapper.delete(wrapper);
        log.info("移除网格员成功，网格ID：{}，用户ID：{}", gridId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddMembers(Long gridId, List<Long> userIds, String memberType) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }
        for (Long userId : userIds) {
            try {
                addMember(gridId, userId, memberType);
            } catch (BusinessException e) {
                log.warn("跳过添加网格员：网格ID={}，用户ID={}，原因：{}", gridId, userId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemoveMembers(Long gridId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("用户ID列表不能为空");
        }
        for (Long userId : userIds) {
            removeMember(gridId, userId);
        }
    }

    @Override
    public List<GridMember> getMembersByGridId(Long gridId) {
        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getGridId, gridId);
        return gridMemberMapper.selectList(wrapper);
    }

    @Override
    public List<GridMember> getGridsByUserId(Long userId) {
        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getUserId, userId);
        return gridMemberMapper.selectList(wrapper);
    }

    private void validateGridAndUser(Long gridId, Long userId) {
        GridInfo gridInfo = gridInfoMapper.selectById(gridId);
        if (gridInfo == null) {
            throw new BusinessException("网格不存在");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
    }
}

package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.SysDept;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.SysDeptMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.DeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept create(SysDept dept) {
        if (StrUtil.isBlank(dept.getName())) {
            throw new BusinessException("部门名称不能为空");
        }
        if (StrUtil.isBlank(dept.getCode())) {
            throw new BusinessException("部门编码不能为空");
        }

        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getCode, dept.getCode());
        Long count = sysDeptMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("部门编码已存在");
        }

        sysDeptMapper.insert(dept);
        log.info("创建部门成功，部门ID：{}，部门名称：{}", dept.getId(), dept.getName());
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDept update(SysDept dept) {
        SysDept exist = sysDeptMapper.selectById(dept.getId());
        if (exist == null) {
            throw new BusinessException("部门不存在");
        }
        sysDeptMapper.updateById(dept);
        log.info("更新部门成功，部门ID：{}", dept.getId());
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept exist = sysDeptMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("部门不存在");
        }

        LambdaQueryWrapper<SysDept> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(SysDept::getParentId, id);
        Long childCount = sysDeptMapper.selectCount(childWrapper);
        if (childCount != null && childCount > 0) {
            throw new BusinessException("存在子部门，无法删除");
        }

        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getDeptId, id);
        Long userCount = sysUserMapper.selectCount(userWrapper);
        if (userCount != null && userCount > 0) {
            throw new BusinessException("部门下存在用户，无法删除");
        }

        sysDeptMapper.deleteById(id);
        log.info("删除部门成功，部门ID：{}", id);
    }

    @Override
    public SysDept getById(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }
        return dept;
    }

    @Override
    public List<SysDept> listAll() {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1);
        wrapper.orderByAsc(SysDept::getSort);
        return sysDeptMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> tree() {
        List<SysDept> allDepts = listAll();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (SysDept dept : allDepts) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", dept.getId());
            node.put("name", dept.getName());
            node.put("code", dept.getCode());
            node.put("parentId", dept.getParentId());
            node.put("leader", dept.getLeader());
            node.put("phone", dept.getPhone());
            node.put("sort", dept.getSort());
            node.put("status", dept.getStatus());
            node.put("children", new ArrayList<>());
            nodes.add(node);
        }
        return buildTree(nodes, 0L);
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> nodes, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> node : nodes) {
            Long nodeParentId = (Long) node.get("parentId");
            if ((parentId == null && nodeParentId == null) || (parentId != null && parentId.equals(nodeParentId))) {
                Long nodeId = (Long) node.get("id");
                List<Map<String, Object>> children = buildTree(nodes, nodeId);
                node.put("children", children);
                result.add(node);
            }
        }
        return result;
    }
}

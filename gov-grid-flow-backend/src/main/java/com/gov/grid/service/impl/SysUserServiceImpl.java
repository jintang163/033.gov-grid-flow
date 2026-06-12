package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.GridMember;
import com.gov.grid.entity.SysUser;
import com.gov.grid.enums.RoleEnum;
import com.gov.grid.mapper.GridMemberMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final GridMemberMapper gridMemberMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SysUser getByUsername(String username) {
        return this.getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getStatus, 1)
        );
    }

    @Override
    public SysUser getByPhone(String phone) {
        return this.getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getPhone, phone)
                        .eq(SysUser::getStatus, 1)
        );
    }

    @Override
    public SysUser getById(Long id) {
        return this.getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getId, id)
                        .eq(SysUser::getStatus, 1)
        );
    }

    @Override
    public PageResult<SysUser> page(Integer pageNum, Integer pageSize, String keyword, String role, Long gridId) {
        pageNum = pageNum != null ? pageNum : 1;
        pageSize = pageSize != null ? pageSize : 10;

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (StrUtil.isNotBlank(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (gridId != null) {
            wrapper.eq(SysUser::getGridId, gridId);
        }

        wrapper.orderByDesc(SysUser::getCreatedAt);
        Page<SysUser> result = sysUserMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(SysUser user) {
        if (StrUtil.isBlank(user.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (StrUtil.isBlank(user.getPassword())) {
            throw new BusinessException("密码不能为空");
        }

        SysUser exist = getByUsername(user.getUsername());
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        sysUserMapper.insert(user);

        if (user.getGridId() != null && StrUtil.isNotBlank(user.getRole())) {
            addGridMember(user.getId(), user.getGridId(), user.getRole());
        }

        log.info("创建用户成功，用户名：{}", user.getUsername());
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateUser(SysUser user) {
        SysUser exist = getById(user.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }

        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }

        sysUserMapper.updateById(user);

        if (user.getGridId() != null && StrUtil.isNotBlank(user.getRole())) {
            updateGridMember(user.getId(), user.getGridId(), user.getRole());
        }

        log.info("更新用户成功，用户ID：{}", user.getId());
        return getById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser exist = getById(id);
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        sysUserMapper.deleteById(id);

        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getUserId, id);
        gridMemberMapper.delete(wrapper);

        log.info("删除用户成功，用户ID：{}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, String role) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRole(role);
        sysUserMapper.updateById(user);

        if (user.getGridId() != null) {
            updateGridMember(userId, user.getGridId(), role);
        }

        log.info("更新用户角色成功，用户ID：{}，新角色：{}", userId, role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserGrid(Long userId, Long gridId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getUserId, userId);
        gridMemberMapper.delete(wrapper);

        user.setGridId(gridId);
        sysUserMapper.updateById(user);

        if (gridId != null && StrUtil.isNotBlank(user.getRole())) {
            addGridMember(userId, gridId, user.getRole());
        }

        log.info("更新用户网格成功，用户ID：{}，新网格ID：{}", userId, gridId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
        log.info("重置密码成功，用户ID：{}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importUsers(MultipartFile file, Long gridId) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, String>> successList = new ArrayList<>();
        List<Map<String, String>> failList = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            ExcelReader reader = ExcelUtil.getReader(is);
            List<List<Object>> rows = reader.read();

            if (rows.size() <= 1) {
                throw new BusinessException("Excel文件无有效数据");
            }

            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                Map<String, String> rowData = new HashMap<>();
                try {
                    String username = getCellString(row.get(0));
                    String realName = getCellString(row.get(1));
                    String phone = getCellString(row.get(2));
                    String email = getCellString(row.get(3));
                    String role = getCellString(row.get(4));
                    String password = getCellString(row.get(5));

                    if (StrUtil.isBlank(username)) {
                        rowData.put("error", "用户名不能为空");
                        failList.add(rowData);
                        continue;
                    }

                    SysUser existUser = getByUsername(username);
                    if (existUser != null) {
                        rowData.put("username", username);
                        rowData.put("error", "用户名已存在");
                        failList.add(rowData);
                        continue;
                    }

                    SysUser user = new SysUser();
                    user.setUsername(username);
                    user.setRealName(realName);
                    user.setPhone(phone);
                    user.setEmail(email);
                    user.setRole(StrUtil.isNotBlank(role) ? role : "worker");
                    user.setPassword(passwordEncoder.encode(StrUtil.isNotBlank(password) ? password : "123456"));
                    user.setGridId(gridId);
                    user.setStatus(1);
                    sysUserMapper.insert(user);

                    if (gridId != null) {
                        addGridMember(user.getId(), gridId, user.getRole());
                    }

                    rowData.put("username", username);
                    rowData.put("realName", realName);
                    successList.add(rowData);
                } catch (Exception e) {
                    rowData.put("error", "解析失败：" + e.getMessage());
                    failList.add(rowData);
                }
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入用户失败", e);
            throw new BusinessException("导入失败：" + e.getMessage());
        }

        result.put("successCount", successList.size());
        result.put("failCount", failList.size());
        result.put("successList", successList);
        result.put("failList", failList);
        return result;
    }

    @Override
    public List<Map<String, String>> getRoleList() {
        List<Map<String, String>> roles = new ArrayList<>();
        for (RoleEnum roleEnum : RoleEnum.values()) {
            Map<String, String> role = new HashMap<>();
            role.put("code", roleEnum.getCode());
            role.put("name", roleEnum.getName());
            roles.add(role);
        }
        return roles;
    }

    private String getCellString(Object cell) {
        if (cell == null) {
            return null;
        }
        return cell.toString().trim();
    }

    private void addGridMember(Long userId, Long gridId, String role) {
        GridMember member = new GridMember();
        member.setUserId(userId);
        member.setGridId(gridId);
        member.setMemberType(role);
        gridMemberMapper.insert(member);
    }

    private void updateGridMember(Long userId, Long gridId, String role) {
        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getUserId, userId);
        List<GridMember> members = gridMemberMapper.selectList(wrapper);

        boolean found = false;
        for (GridMember member : members) {
            if (member.getGridId().equals(gridId)) {
                member.setMemberType(role);
                gridMemberMapper.updateById(member);
                found = true;
                break;
            }
        }

        if (!found) {
            gridMemberMapper.delete(wrapper);
            addGridMember(userId, gridId, role);
        }
    }
}

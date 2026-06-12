package com.gov.grid.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.grid.common.PageResult;
import com.gov.grid.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    SysUser getByPhone(String phone);

    SysUser getById(Long id);

    PageResult<SysUser> page(Integer pageNum, Integer pageSize, String keyword, String role, Long gridId);

    SysUser createUser(SysUser user);

    SysUser updateUser(SysUser user);

    void deleteUser(Long id);

    void updateUserRole(Long userId, String role);

    void updateUserGrid(Long userId, Long gridId);

    void resetPassword(Long userId, String newPassword);

    Map<String, Object> importUsers(MultipartFile file, Long gridId);

    List<Map<String, String>> getRoleList();
}

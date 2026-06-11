package com.gov.grid.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.grid.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);

    SysUser getByPhone(String phone);

    SysUser getById(Long id);
}

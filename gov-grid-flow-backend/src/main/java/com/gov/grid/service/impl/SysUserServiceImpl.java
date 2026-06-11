package com.gov.grid.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

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
}

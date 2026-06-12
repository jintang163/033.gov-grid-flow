package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.entity.SysUser;
import com.gov.grid.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;

    @ApiOperation("分页查询用户列表")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long gridId) {
        PageResult<SysUser> pageResult = sysUserService.page(pageNum, pageSize, keyword, role, gridId);
        return Result.success(pageResult);
    }

    @ApiOperation("获取用户详情")
    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        return Result.success(user);
    }

    @ApiOperation("新增用户")
    @PostMapping
    public Result<SysUser> create(@RequestBody SysUser user) {
        SysUser created = sysUserService.createUser(user);
        return Result.success(created);
    }

    @ApiOperation("修改用户")
    @PutMapping
    public Result<SysUser> update(@RequestBody SysUser user) {
        SysUser updated = sysUserService.updateUser(user);
        return Result.success(updated);
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    @ApiOperation("分配角色")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam String role) {
        sysUserService.updateUserRole(id, role);
        return Result.success();
    }

    @ApiOperation("分配网格")
    @PutMapping("/{id}/grid")
    public Result<Void> updateGrid(@PathVariable Long id, @RequestParam Long gridId) {
        sysUserService.updateUserGrid(id, gridId);
        return Result.success();
    }

    @ApiOperation("重置密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        sysUserService.resetPassword(id, password);
        return Result.success();
    }

    @ApiOperation("批量导入用户")
    @PostMapping("/import")
    public Result<Map<String, Object>> importUsers(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long gridId) {
        Map<String, Object> result = sysUserService.importUsers(file, gridId);
        return Result.success(result);
    }

    @ApiOperation("获取角色列表")
    @GetMapping("/roles")
    public Result<List<Map<String, String>>> getRoleList() {
        List<Map<String, String>> roles = sysUserService.getRoleList();
        return Result.success(roles);
    }
}

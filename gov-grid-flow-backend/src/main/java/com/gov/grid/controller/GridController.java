package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysUser;
import com.gov.grid.service.GridService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "网格管理")
@RestController
@RequestMapping("/grid")
@RequiredArgsConstructor
public class GridController {

    private final GridService gridService;

    @ApiOperation("获取所有网格列表")
    @GetMapping("/list")
    public Result<List<GridInfo>> listAll() {
        List<GridInfo> list = gridService.listAll();
        return Result.success(list);
    }

    @ApiOperation("获取网格树形结构")
    @GetMapping("/tree")
    public Result<List<GridInfo>> getGridTree() {
        List<GridInfo> tree = gridService.getGridTree();
        return Result.success(tree);
    }

    @ApiOperation("按层级获取网格树")
    @GetMapping("/tree/level")
    public Result<List<GridInfo>> getGridTreeByLevel(@RequestParam Integer level) {
        List<GridInfo> tree = gridService.getGridTreeByLevel(level);
        return Result.success(tree);
    }

    @ApiOperation("获取子网格列表")
    @GetMapping("/children")
    public Result<List<GridInfo>> getChildren(@RequestParam(required = false) Long parentId) {
        List<GridInfo> children = gridService.getChildren(parentId);
        return Result.success(children);
    }

    @ApiOperation("根据ID获取单个网格")
    @GetMapping("/{id}")
    public Result<GridInfo> getById(@PathVariable Long id) {
        GridInfo gridInfo = gridService.getById(id);
        return Result.success(gridInfo);
    }

    @ApiOperation("获取网格成员列表")
    @GetMapping("/members")
    public Result<List<SysUser>> getMembers(@RequestParam Long gridId) {
        List<SysUser> members = gridService.getMembers(gridId);
        return Result.success(members);
    }

    @ApiOperation("分页查询网格列表")
    @GetMapping("/page")
    public Result<PageResult<GridInfo>> page(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword) {
        PageResult<GridInfo> pageResult = gridService.page(pageNum, pageSize, keyword);
        return Result.success(pageResult);
    }

    @ApiOperation("新增网格")
    @PostMapping
    @PreAuthorize("hasRole('admin') or hasRole('street_manager')")
    public Result<GridInfo> create(@RequestBody GridInfo gridInfo) {
        GridInfo created = gridService.create(gridInfo);
        return Result.success(created);
    }

    @ApiOperation("修改网格")
    @PutMapping
    @PreAuthorize("hasRole('admin') or hasRole('street_manager')")
    public Result<GridInfo> update(@RequestBody GridInfo gridInfo) {
        GridInfo updated = gridService.update(gridInfo);
        return Result.success(updated);
    }

    @ApiOperation("删除网格")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@PathVariable Long id) {
        gridService.delete(id);
        return Result.success();
    }
}

package com.gov.grid.security;

import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysUser;
import com.gov.grid.enums.RoleEnum;
import com.gov.grid.service.GridService;
import com.gov.grid.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataScopeUtils {

    private static SysUserService sysUserService;
    private static GridService gridService;

    @Autowired
    public void setSysUserService(SysUserService sysUserService) {
        DataScopeUtils.sysUserService = sysUserService;
    }

    @Autowired
    public void setGridService(GridService gridService) {
        DataScopeUtils.gridService = gridService;
    }

    public static List<Long> getAccessibleGridIds() {
        Long currentUserId = JwtAuthenticationTokenFilter.getCurrentUserId();
        if (currentUserId == null) {
            return Collections.emptyList();
        }

        SysUser user = sysUserService.getById(currentUserId);
        if (user == null) {
            return Collections.emptyList();
        }

        String role = user.getRole();
        RoleEnum roleEnum = RoleEnum.getByCode(role);

        if (roleEnum == null) {
            return Collections.emptyList();
        }

        switch (roleEnum) {
            case ADMIN:
                return getAllGridIds();
            case STREET_MANAGER:
            case SUPERVISOR:
                return getSubGridIds(user.getGridId());
            case GRID_LEADER:
                return getSubGridIds(user.getGridId());
            case WORKER:
                if (user.getGridId() != null) {
                    return Collections.singletonList(user.getGridId());
                }
                return Collections.emptyList();
            case HANDLER:
                return getAllGridIds();
            default:
                return Collections.emptyList();
        }
    }

    public static boolean canAccessGrid(Long gridId) {
        if (gridId == null) {
            return false;
        }
        List<Long> accessibleGridIds = getAccessibleGridIds();
        return accessibleGridIds.contains(gridId);
    }

    private static List<Long> getAllGridIds() {
        List<GridInfo> allGrids = gridService.listAll();
        return allGrids.stream().map(GridInfo::getId).collect(Collectors.toList());
    }

    private static List<Long> getSubGridIds(Long rootGridId) {
        if (rootGridId == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        result.add(rootGridId);
        collectChildGridIds(rootGridId, result);
        return result;
    }

    private static void collectChildGridIds(Long parentId, List<Long> result) {
        List<GridInfo> children = gridService.getChildren(parentId);
        if (children != null && !children.isEmpty()) {
            for (GridInfo child : children) {
                result.add(child.getId());
                collectChildGridIds(child.getId(), result);
            }
        }
    }

    public static String getCurrentUserRole() {
        Long currentUserId = JwtAuthenticationTokenFilter.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(currentUserId);
        return user != null ? user.getRole() : null;
    }

    public static Long getCurrentUserGridId() {
        Long currentUserId = JwtAuthenticationTokenFilter.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }
        SysUser user = sysUserService.getById(currentUserId);
        return user != null ? user.getGridId() : null;
    }
}

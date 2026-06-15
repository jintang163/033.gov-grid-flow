package com.gov.grid.controller;

import com.gov.grid.annotation.AuditLog;
import com.gov.grid.common.Result;
import com.gov.grid.dto.LoginDTO;
import com.gov.grid.entity.SysUser;
import com.gov.grid.service.AuthService;
import com.gov.grid.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "认证接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    @ApiOperation("用户名密码登录")
    @PostMapping("/login")
    @AuditLog(module = "auth", operation = "login", description = "用户登录", recordParams = false)
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.success(loginVO);
    }

    @ApiOperation("登出")
    @PostMapping("/logout")
    @AuditLog(module = "auth", operation = "logout", description = "用户登出")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(token);
        return Result.success();
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/userinfo")
    public Result<SysUser> getUserInfo(HttpServletRequest request) {
        String token = extractToken(request);
        SysUser user = authService.getUserInfo(token);
        return Result.success(user);
    }

    @ApiOperation("发送验证码")
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestParam String phone) {
        authService.sendCode(phone);
        return Result.success("验证码发送成功", null);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(header);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(prefix + " ")) {
            return authHeader.substring(prefix.length() + 1);
        }
        return null;
    }
}

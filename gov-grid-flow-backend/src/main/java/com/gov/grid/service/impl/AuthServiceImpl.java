package com.gov.grid.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.LoginDTO;
import com.gov.grid.entity.SysUser;
import com.gov.grid.service.AuthService;
import com.gov.grid.service.SysUserService;
import com.gov.grid.utils.JwtUtils;
import com.gov.grid.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.expiration}")
    private Long tokenExpiration;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        String captchaKey = CAPTCHA_KEY_PREFIX + loginDTO.getUsername();
        Object cachedCode = redisTemplate.opsForValue().get(captchaKey);

        if (cachedCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }

        if (!StrUtil.equalsIgnoreCase(loginDTO.getCode(), cachedCode.toString())) {
            throw new BusinessException("验证码错误");
        }

        redisTemplate.delete(captchaKey);

        SysUser user = sysUserService.getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        user.setPassword(null);

        return LoginVO.builder()
                .token(token)
                .user(user)
                .build();
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isNotBlank(token) && jwtUtils.validateToken(token)) {
            long expireTime = jwtUtils.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
            if (expireTime > 0) {
                redisTemplate.opsForValue().set(TOKEN_BLACKLIST_PREFIX + token, "1", expireTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public SysUser getUserInfo(String token) {
        if (StrUtil.isBlank(token) || !jwtUtils.validateToken(token)) {
            throw new BusinessException("Token无效或已过期");
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
            throw new BusinessException("Token已失效，请重新登录");
        }

        String username = jwtUtils.getUsernameFromToken(token);
        SysUser user = sysUserService.getByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void sendCode(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new BusinessException("手机号不能为空");
        }

        SysUser user = sysUserService.getByPhone(phone);
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }

        String code = RandomUtil.randomNumbers(6);
        String key = CAPTCHA_KEY_PREFIX + user.getUsername();

        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        System.out.println("发送验证码到手机号: " + phone + ", 验证码: " + code);
    }
}

package com.gov.grid.service;

import com.gov.grid.dto.LoginDTO;
import com.gov.grid.entity.SysUser;
import com.gov.grid.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO loginDTO);

    void logout(String token);

    SysUser getUserInfo(String token);

    void sendCode(String phone);
}

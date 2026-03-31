package com.xnl.qc.service;

import com.xnl.qc.dto.Dto.ChangeMyPwRequest;
import com.xnl.qc.dto.Dto.LoginRequest;
import com.xnl.qc.dto.Dto.LoginResponse;

public interface AuthService {

    /**
     * 护士登录验证。
     * 密码优先级：① 该护士的个人密码 ② 管理员密码（万能登录）
     */
    LoginResponse login(LoginRequest req);

    /**
     * 护士修改自己的登录密码。
     * @param employeeId 当前登录护士工号（从 JWT 取）
     */
    void changeMyPassword(String employeeId, ChangeMyPwRequest req);
}

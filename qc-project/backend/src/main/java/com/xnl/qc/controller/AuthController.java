package com.xnl.qc.controller;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/login — 登录，返回 JWT */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    /** POST /api/auth/change-password — 护士修改自己的密码（需 JWT） */
    @PostMapping("/change-password")
    public Result<Void> changeMyPassword(@Valid @RequestBody ChangeMyPwRequest req,
                                          HttpServletRequest httpReq) {
        String eid = (String) httpReq.getAttribute("employeeId");
        authService.changeMyPassword(eid, req);
        return Result.ok();
    }
}

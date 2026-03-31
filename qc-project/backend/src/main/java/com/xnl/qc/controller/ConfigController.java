package com.xnl.qc.controller;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    /** POST /api/config/admin-password — 管理员：修改管理员密码 */
    @PostMapping("/admin-password")
    public Result<Void> changeAdminPassword(@Valid @RequestBody ChangeAdminPwRequest body,
                                             HttpServletRequest req) {
        requireAdmin(req);
        configService.changeAdminPassword(body);
        return Result.ok();
    }

    private void requireAdmin(HttpServletRequest req) {
        if (!Boolean.TRUE.equals(req.getAttribute("admin")))
            throw new BusinessException(403, "无权限，需要管理员身份");
    }
}

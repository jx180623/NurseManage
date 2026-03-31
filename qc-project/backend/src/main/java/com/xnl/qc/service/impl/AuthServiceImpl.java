package com.xnl.qc.service.impl;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.entity.Nurse;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.repository.NurseRepository;
import com.xnl.qc.repository.SysConfigRepository;
import com.xnl.qc.security.JwtUtil;
import com.xnl.qc.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_PW = "XNL226";

    private final NurseRepository     nurseRepository;
    private final SysConfigRepository configRepository;
    private final JwtUtil              jwtUtil;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        // 1. 查工号（不区分大小写）
        Nurse nurse = nurseRepository
                .findByEmployeeIdIgnoreCase(req.getEmployeeId())
                .orElseThrow(() -> new BusinessException("工号不存在，请检查后重试"));

        if (!nurse.getEnabled()) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
        }

        // 2. 获取管理员密码
        String adminPw = configRepository.findById("admin_password")
                .map(c -> c.getConfigValue()).orElse("admin888");

        // 3. 密码验证：个人密码 OR 管理员密码
        String nursePw = StringUtils.hasText(nurse.getPassword())
                ? nurse.getPassword() : DEFAULT_PW;

        boolean isAdmin = req.getPassword().equals(adminPw);
        if (!req.getPassword().equals(nursePw) && !isAdmin) {
            log.warn("登录失败 - 工号: {}", req.getEmployeeId());
            throw new BusinessException("密码错误，请重新输入");
        }

        // 4. 签发 JWT
        String token = jwtUtil.generate(nurse.getEmployeeId(), nurse.getName(), isAdmin);
        log.info("登录成功 - 工号: {}, 管理员: {}", nurse.getEmployeeId(), isAdmin);

        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setEmployeeId(nurse.getEmployeeId());
        resp.setName(nurse.getName());
        resp.setAdmin(isAdmin);
        return resp;
    }

    @Override
    @Transactional
    public void changeMyPassword(String employeeId, ChangeMyPwRequest req) {
        Nurse nurse = nurseRepository.findByEmployeeIdIgnoreCase(employeeId)
                .orElseThrow(() -> new BusinessException("账号不存在"));

        // 验证当前密码（个人密码 OR 管理员密码均可）
        String adminPw = configRepository.findById("admin_password")
                .map(c -> c.getConfigValue()).orElse("admin888");
        String currentStored = StringUtils.hasText(nurse.getPassword())
                ? nurse.getPassword() : DEFAULT_PW;

        if (!req.getCurrentPassword().equals(currentStored)
                && !req.getCurrentPassword().equals(adminPw)) {
            throw new BusinessException("当前密码验证失败，请重新输入");
        }
        if (req.getNewPassword().equals(req.getCurrentPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        if (req.getNewPassword().length() < 4) {
            throw new BusinessException("新密码至少需要 4 位");
        }

        nurse.setPassword(req.getNewPassword());
        nurseRepository.save(nurse);
        log.info("护士修改密码成功 - 工号: {}", employeeId);
    }
}

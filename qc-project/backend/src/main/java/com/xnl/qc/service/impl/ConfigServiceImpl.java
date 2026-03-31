package com.xnl.qc.service.impl;

import com.xnl.qc.dto.Dto.ChangeAdminPwRequest;
import com.xnl.qc.entity.SysConfig;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.repository.SysConfigRepository;
import com.xnl.qc.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private static final String KEY_ADMIN = "admin_password";

    private final SysConfigRepository configRepository;

    @Override
    @Transactional(readOnly = true)
    public String getValue(String key) {
        return configRepository.findById(key)
                .map(SysConfig::getConfigValue).orElse(null);
    }

    @Override
    @Transactional
    public void changeAdminPassword(ChangeAdminPwRequest req) {
        if (req.getNewPw().length() < 4) {
            throw new BusinessException("新密码至少需要 4 位");
        }
        String current = configRepository.findById(KEY_ADMIN)
                .map(SysConfig::getConfigValue).orElse("admin888");

        if (!req.getCurrentPw().equals(current)) {
            throw new BusinessException("当前管理员密码验证失败");
        }
        if (req.getNewPw().equals(req.getCurrentPw())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }

        SysConfig cfg = configRepository.findById(KEY_ADMIN).orElse(new SysConfig());
        cfg.setConfigKey(KEY_ADMIN);
        cfg.setConfigValue(req.getNewPw());
        cfg.setRemark("管理员密码");
        configRepository.save(cfg);
        log.info("管理员密码已修改");
    }
}

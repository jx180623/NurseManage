package com.xnl.qc.service;

import com.xnl.qc.dto.Dto.ChangeAdminPwRequest;

/**
 * 系统配置服务（仅管理员密码，护士密码已移至 NurseService）
 */
public interface ConfigService {

    String getValue(String key);

    /** 修改管理员密码，需验证当前管理员密码 */
    void changeAdminPassword(ChangeAdminPwRequest req);
}

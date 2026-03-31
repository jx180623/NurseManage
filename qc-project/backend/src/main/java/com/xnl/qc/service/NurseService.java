package com.xnl.qc.service;

import com.xnl.qc.dto.Dto.*;
import java.util.List;

public interface NurseService {

    /** 获取所有【启用】护士列表（登录 & 多选用） */
    List<NurseDto> listEnabled();

    /** 获取全部护士（含禁用），管理员用 */
    List<NurseDto> listAll();

    /** 新增护士 */
    NurseDto create(NurseCreateRequest req);

    /** 修改护士基本信息（工号、姓名、启用状态） */
    NurseDto update(Long id, NurseUpdateRequest req);

    /** 管理员重置指定护士密码（为空则重置为默认密码 XNL226） */
    void resetPassword(ResetNursePwRequest req);

    /** 删除护士 */
    void delete(Long id);
}

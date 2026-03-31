package com.xnl.qc.controller;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.service.NurseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nurses")
@RequiredArgsConstructor
public class NurseController {

    private final NurseService nurseService;

    /** GET /api/nurses/list — 公开，启用护士列表（登录 & 多选用） */
    @GetMapping("/list")
    public Result<List<NurseDto>> list() {
        return Result.ok(nurseService.listEnabled());
    }

    /** GET /api/nurses/all — 管理员：全部护士（含密码状态） */
    @GetMapping("/all")
    public Result<List<NurseDto>> all(HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(nurseService.listAll());
    }

    /** POST /api/nurses — 管理员：新增护士 */
    @PostMapping
    public Result<NurseDto> create(@Valid @RequestBody NurseCreateRequest body,
                                   HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(nurseService.create(body));
    }

    /** PUT /api/nurses/{id} — 管理员：修改护士信息 */
    @PutMapping("/{id}")
    public Result<NurseDto> update(@PathVariable Long id,
                                   @Valid @RequestBody NurseUpdateRequest body,
                                   HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(nurseService.update(id, body));
    }

    /** POST /api/nurses/reset-password — 管理员：重置指定护士密码 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetNursePwRequest body,
                                      HttpServletRequest req) {
        requireAdmin(req);
        nurseService.resetPassword(body);
        return Result.ok();
    }

    /** DELETE /api/nurses/{id} — 管理员：删除护士 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        nurseService.delete(id);
        return Result.ok();
    }

    private void requireAdmin(HttpServletRequest req) {
        if (!Boolean.TRUE.equals(req.getAttribute("admin")))
            throw new BusinessException(403, "无权限，需要管理员身份");
    }
}

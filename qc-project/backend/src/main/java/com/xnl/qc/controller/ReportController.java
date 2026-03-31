package com.xnl.qc.controller;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * POST /api/reports  （需要护士 JWT）
     * 护士提交或更新当日质控日报（同一天重复提交则覆盖）
     */
    @PostMapping
    public Result<ReportResponse> submit(@Valid @RequestBody ReportRequest body,
                                         HttpServletRequest req) {
        String eid  = (String) req.getAttribute("employeeId");
        String name = (String) req.getAttribute("name");
        return Result.ok(reportService.submitOrUpdate(eid, name, body));
    }

    /**
     * GET /api/reports/mine  （需要护士 JWT）
     * 查询当前护士最近 8 份历史日报（含条目）
     */
    @GetMapping("/mine")
    public Result<List<ReportResponse>> mine(HttpServletRequest req) {
        String eid = (String) req.getAttribute("employeeId");
        return Result.ok(reportService.myHistory(eid, 8));
    }

    /**
     * GET /api/reports/by-date/{date}  （需要护士 JWT）
     * 按日期查询当前护士的日报，用于历史记录编辑回填
     */
    @GetMapping("/by-date/{date}")
    public Result<ReportResponse> byDate(@PathVariable String date,
                                          HttpServletRequest req) {
        String eid = (String) req.getAttribute("employeeId");
        LocalDate localDate = parseDate(date);
        ReportResponse resp = reportService.getByDate(eid, localDate);
        if (resp == null) {
            return Result.fail(404, "该日期暂无您的质控记录");
        }
        return Result.ok(resp);
    }

    /**
     * GET /api/reports/admin/all  （需要管理员 JWT）
     * 管理员查询所有日报（支持 start/end 日期范围筛选）
     */
    @GetMapping("/admin/all")
    public Result<List<ReportResponse>> adminAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(reportService.adminQueryAll(start, end));
    }

    /**
     * DELETE /api/reports/{id}  （需要管理员 JWT）
     * 管理员删除指定日报（级联删除条目）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest req) {
        requireAdmin(req);
        reportService.deleteReport(id);
        return Result.ok();
    }

    /**
     * GET /api/reports/admin/export  （需要管理员 JWT）
     * 导出 Excel，支持 start/end 日期参数（格式 yyyy-MM-dd）
     * 直接写入响应流，前端触发文件下载
     */
    @GetMapping("/admin/export")
    public void export(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            HttpServletRequest req,
            HttpServletResponse res) throws IOException {
        requireAdmin(req);
        LocalDate s = start != null ? parseDate(start) : null;
        LocalDate e = end   != null ? parseDate(end)   : null;
        reportService.exportExcel(s, e, res);
    }

    // ── 辅助方法 ──────────────────────────────────────

    private void requireAdmin(HttpServletRequest req) {
        if (!Boolean.TRUE.equals(req.getAttribute("admin"))) {
            throw new BusinessException(403, "无权限，需要管理员身份");
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception ex) {
            throw new BusinessException("日期格式错误，请使用 yyyy-MM-dd 格式");
        }
    }
}

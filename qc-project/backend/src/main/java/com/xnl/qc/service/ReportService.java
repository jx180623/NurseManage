package com.xnl.qc.service;

import com.xnl.qc.dto.Dto.ReportRequest;
import com.xnl.qc.dto.Dto.ReportResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /**
     * 护士提交/更新日报（upsert：同一护士同一天只保留一份）
     * 前端已保证每天只提交一次，此接口同时兼容修改
     */
    ReportResponse submitOrUpdate(String submitterId, String submitterName, ReportRequest req);

    /** 当前护士最近 N 份历史日报（不含今日，按日期倒序） */
    List<ReportResponse> myHistory(String submitterId, int limit);

    /** 按日期查当前护士日报（含条目） */
    ReportResponse getByDate(String submitterId, LocalDate date);

    /** 管理员：查询所有日报，支持日期范围筛选 */
    List<ReportResponse> adminQueryAll(LocalDate start, LocalDate end);

    /** 管理员：删除指定日报 */
    void deleteReport(Long id);

    /** 管理员：导出 Excel */
    void exportExcel(LocalDate start, LocalDate end, HttpServletResponse response) throws IOException;
}

package com.xnl.qc.service.impl;

import com.xnl.qc.dto.Dto.*;
import com.xnl.qc.entity.DayReport;
import com.xnl.qc.entity.QcItem;
import com.xnl.qc.exception.BusinessException;
import com.xnl.qc.repository.DayReportRepository;
import com.xnl.qc.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** 与前端 FREE_INPUT_KEYS 完全对应 */
    private static final Set<String> FREE_INPUT_KEYS = Set.of(
        "basic_0",  // 科室患者基础护理质量（三短六洁、床单位情况）
        "basic_1",  // 护理员能说出重点患者的风险因素：一级、压疮、高跌、管路、皮肤
        "fall_8"    // 护理员能说出分管高跌患者及风险因素
    );

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DayReportRepository reportRepository;

    // ── 提交/更新 ────────────────────────────────────────

    @Override
    @Transactional
    public ReportResponse submitOrUpdate(String submitterId, String submitterName, ReportRequest req) {
        LocalDate date = parseDate(req.getReportDate());

        // 全科唯一：按日期查，不再按填报人区分
        DayReport report = reportRepository.findByReportDate(date)
                .orElse(new DayReport());

        // 记录本次操作的填报人（新建时设置，修改时更新为最后操作人）
        report.setReportDate(date);
        report.setSubmitterId(submitterId);
        report.setSubmitterName(submitterName);
        report.setQcPerson(req.getQcPerson() != null ? req.getQcPerson().trim() : submitterName);
        fillAdmission(report, req.getAdmData());

        report.getItems().clear();
        if (req.getItems() != null) {
            for (QcItemDto dto : req.getItems()) {
                report.getItems().add(buildItem(report, dto));
            }
        }

        DayReport saved = reportRepository.save(report);
        log.info("日报保存 - 填报人: {}, 日期: {}, 条目: {}", submitterId, date, saved.getItems().size());
        return toResponse(saved);
    }

    // ── 护士端查询（全科共享，不再过滤填报人） ──────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> myHistory(String submitterId, int limit) {
        // 全科共享：返回所有人的历史，按日期倒序，取最近 N 条
        return reportRepository.findAllWithItems()
                .stream().limit(limit).map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getByDate(String submitterId, LocalDate date) {
        // 全科唯一：只按日期查，不过滤填报人
        return reportRepository.findByReportDate(date)
                .flatMap(r -> reportRepository.findByIdWithItems(r.getId()))
                .map(this::toResponse).orElse(null);
    }

    // ── 管理员端 ──────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> adminQueryAll(LocalDate start, LocalDate end) {
        List<DayReport> list = (start != null && end != null)
                ? reportRepository.findByReportDateBetweenOrderByReportDateDesc(start, end)
                : reportRepository.findAllByOrderByReportDateDesc();
        return list.stream()
                .map(r -> reportRepository.findByIdWithItems(r.getId()).orElse(r))
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReport(Long id) {
        DayReport r = reportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("日报（ID=" + id + "）不存在"));
        reportRepository.delete(r);
        log.info("日报删除 - ID: {}, 填报人: {}, 日期: {}", id, r.getSubmitterId(), r.getReportDate());
    }

    // ── Excel 导出 ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void exportExcel(LocalDate start, LocalDate end, HttpServletResponse response) throws IOException {
        List<DayReport> list = (start != null && end != null)
                ? reportRepository.findByReportDateBetweenOrderByReportDateDesc(start, end)
                : reportRepository.findAllByOrderByReportDateDesc();
        list = list.stream()
                .map(r -> reportRepository.findByIdWithItems(r.getId()).orElse(r))
                .collect(Collectors.toList());

        String fileName = URLEncoder.encode("心内六科质控数据_" + LocalDate.now() + ".xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("质控数据");
            CellStyle hStyle = createHeaderStyle(wb);

            String[] cols = {
                "质控日期","填报人工号","填报人姓名","质控者",
                "昨日患者总人数","今日患者总人数","入院人数","转入人数",
                "出院人数","转出人数","急诊转入人数","手术人数",
                "一级人数","四级手术人数","陪护人数","陪护率",
                "维度","查检条目","姓名录入方式","已完成",
                "质控人数","问题人数","不适用人数","问题汇总","原因分析","备注","被质控者","提交时间","最后修改"
            };
            Row hRow = sheet.createRow(0);
            for (int i = 0; i < cols.length; i++) {
                Cell c = hRow.createCell(i); c.setCellValue(cols[i]); c.setCellStyle(hStyle);
            }

            int rowNum = 1;
            for (DayReport r : list) {
                for (QcItem it : r.getItems()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(s(r.getReportDate()));
                    row.createCell(1).setCellValue(s(r.getSubmitterId()));
                    row.createCell(2).setCellValue(s(r.getSubmitterName()));
                    row.createCell(3).setCellValue(s(r.getQcPerson()));
                    setCellInt(row, 4, r.getAdmYesterday());
                    setCellInt(row, 5, r.getAdmToday());
                    setCellInt(row, 6, r.getAdmIn());
                    setCellInt(row, 7, r.getAdmTransferIn());
                    setCellInt(row, 8, r.getAdmOut());
                    setCellInt(row, 9, r.getAdmTransferOut());
                    setCellInt(row, 10, r.getAdmEr());
                    setCellInt(row, 11, r.getAdmSurgery());
                    setCellInt(row, 12, r.getAdmLevel1());
                    setCellInt(row, 13, r.getAdmLevel4());
                    setCellInt(row, 14, r.getAdmEscort());
                    row.createCell(15).setCellValue(s(r.getAdmRate()));
                    row.createCell(16).setCellValue(s(it.getCategory()));
                    row.createCell(17).setCellValue(s(it.getItemText()));
                    row.createCell(18).setCellValue("free".equals(it.getNamesInputType()) ? "手工填写" : "多选");
                    row.createCell(19).setCellValue(Boolean.TRUE.equals(it.getChecked()) ? "是" : "否");
                    setCellInt(row, 20, it.getQcCount());
                    setCellInt(row, 21, it.getIssueCount());
                    setCellInt(row, 22, it.getNaCount());
                    row.createCell(23).setCellValue(s(it.getIssueSummary()));
                    row.createCell(24).setCellValue(s(it.getRootAnalysis()));
                    row.createCell(25).setCellValue(s(it.getRemark()));
                    row.createCell(26).setCellValue(s(it.getNursesInvolved()));
                    row.createCell(27).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : "");
                    row.createCell(28).setCellValue(r.getLastEdit()  != null ? r.getLastEdit().format(DT_FMT)  : "");
                }
            }
            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) * 2, 255 * 256));
            }
            wb.write(response.getOutputStream());
        }
        log.info("Excel 导出完成 - {} ~ {}, {} 份日报", start, end, list.size());
    }

    // ── 私有工具 ──────────────────────────────────────────

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new BusinessException("日期格式错误，请使用 yyyy-MM-dd：" + s); }
    }

    private void fillAdmission(DayReport r, AdmDataDto d) {
        if (d == null) return;
        r.setAdmYesterday(d.getYesterdayTotal());
        r.setAdmToday(d.getTodayTotal());
        r.setAdmIn(d.getIn());
        r.setAdmTransferIn(d.getTransferIn());
        r.setAdmOut(d.getOut());
        r.setAdmTransferOut(d.getTransferOut());
        r.setAdmEr(d.getEr());
        r.setAdmSurgery(d.getSurgery());
        r.setAdmLevel1(d.getLevel1());
        r.setAdmLevel4(d.getLevel4());
        r.setAdmEscort(d.getEscort());
        r.setAdmRate(d.getRate());
    }

    private QcItem buildItem(DayReport report, QcItemDto dto) {
        QcItem item = new QcItem();
        item.setReport(report);
        item.setCategory(safe(dto.getCategory()));
        item.setItemKey(safe(dto.getItemKey()));
        item.setItemText(safe(dto.getItemText()));
        item.setNamesInputType(FREE_INPUT_KEYS.contains(dto.getItemKey()) ? "free" : "multi");
        item.setChecked(Boolean.TRUE.equals(dto.getChecked()));
        item.setQcCount(dto.getCount());
        item.setIssueCount(dto.getIssueCount());
        item.setNaCount(dto.getNaCount());
        item.setIssueSummary(dto.getIssueSummary());
        item.setRootAnalysis(dto.getRootAnalysis());
        item.setRemark(dto.getRemark());
        item.setNursesInvolved(dto.getNames());
        return item;
    }

    private AdmDataDto buildAdmDto(DayReport r) {
        AdmDataDto d = new AdmDataDto();
        d.setYesterdayTotal(r.getAdmYesterday());
        d.setTodayTotal(r.getAdmToday());
        d.setIn(r.getAdmIn());
        d.setTransferIn(r.getAdmTransferIn());
        d.setOut(r.getAdmOut());
        d.setTransferOut(r.getAdmTransferOut());
        d.setEr(r.getAdmEr());
        d.setSurgery(r.getAdmSurgery());
        d.setLevel1(r.getAdmLevel1());
        d.setLevel4(r.getAdmLevel4());
        d.setEscort(r.getAdmEscort());
        d.setRate(r.getAdmRate());
        return d;
    }

    private QcItemDto buildItemDto(QcItem it) {
        QcItemDto dto = new QcItemDto();
        dto.setItemKey(it.getItemKey());
        dto.setCategory(it.getCategory());
        dto.setItemText(it.getItemText());
        dto.setNamesInputType(it.getNamesInputType());
        dto.setChecked(it.getChecked());
        dto.setCount(it.getQcCount());
        dto.setIssueCount(it.getIssueCount());
        dto.setNaCount(it.getNaCount());
        dto.setIssueSummary(it.getIssueSummary());
        dto.setRootAnalysis(it.getRootAnalysis());
        dto.setRemark(it.getRemark());
        dto.setNames(it.getNursesInvolved());
        return dto;
    }

    private ReportResponse toResponse(DayReport r) {
        ReportResponse resp = new ReportResponse();
        resp.setId(r.getId());
        resp.setReportDate(r.getReportDate() != null ? r.getReportDate().toString() : null);
        resp.setSubmitterId(r.getSubmitterId());
        resp.setSubmitterName(r.getSubmitterName());
        resp.setQcPerson(r.getQcPerson());
        resp.setAdmData(buildAdmDto(r));
        resp.setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt().format(DT_FMT) : null);
        resp.setLastEdit(r.getLastEdit()   != null ? r.getLastEdit().format(DT_FMT)  : null);
        resp.setItems(r.getItems().stream().map(this::buildItemDto).collect(Collectors.toList()));
        return resp;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)11);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN); s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);  s.setBorderRight(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private void setCellInt(Row row, int col, Integer val) {
        Cell c = row.createCell(col);
        if (val != null) c.setCellValue(val); else c.setCellValue("");
    }

    private String s(Object v) { return v != null ? v.toString() : ""; }
    private String safe(String v) { return v != null ? v : ""; }
}

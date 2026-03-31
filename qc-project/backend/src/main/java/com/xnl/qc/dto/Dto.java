package com.xnl.qc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/** 统一 DTO 定义（与最新前端 HTML 完全对齐） */
public class Dto {

    // ── Auth ─────────────────────────────────────────────
    @Data
    public static class LoginRequest {
        @NotBlank private String employeeId;
        @NotBlank private String password;
    }

    @Data
    public static class LoginResponse {
        private String  token;
        private String  employeeId;
        private String  name;
        private boolean admin;
    }

    // ── Nurse ─────────────────────────────────────────────
    @Data
    public static class NurseDto {
        private Long    id;
        private String  employeeId;
        private String  name;
        private Boolean enabled;
        /** 是否已修改个人密码（true=非默认密码） */
        private boolean hasCustomPassword;
    }

    @Data
    public static class NurseCreateRequest {
        @NotBlank private String employeeId;
        @NotBlank private String name;
        /** 初始密码，为空则用默认密码 XNL226 */
        private String password;
    }

    @Data
    public static class NurseUpdateRequest {
        @NotBlank private String  employeeId;
        @NotBlank private String  name;
        private Boolean enabled;
    }

    /** 管理员重置指定护士密码 */
    @Data
    public static class ResetNursePwRequest {
        /** 目标护士ID */
        @NotBlank private String employeeId;
        /** 新密码；为空则重置为默认密码 */
        private String newPassword;
    }

    /** 护士自己修改个人密码 */
    @Data
    public static class ChangeMyPwRequest {
        @NotBlank private String currentPassword;
        @NotBlank @Size(min = 4) private String newPassword;
    }

    // ── Password（管理员密码修改）────────────────────────
    @Data
    public static class ChangeAdminPwRequest {
        @NotBlank private String currentPw;
        @NotBlank @Size(min = 4) private String newPw;
    }

    // ── Report ────────────────────────────────────────────

    /** 出入院维度数据（与前端 ADM 字段一一对应） */
    @Data
    public static class AdmDataDto {
        private Integer yesterdayTotal;   // 昨日患者总人数
        private Integer todayTotal;       // 今日患者总人数（前端自动计算后提交）
        private Integer in;
        private Integer transferIn;
        private Integer out;
        private Integer transferOut;
        private Integer er;
        private Integer surgery;
        private Integer level1;
        private Integer level4;
        private Integer escort;
        private String  rate;             // 陪护率，如 "62.5%"
    }

    /** 质控条目 DTO */
    @Data
    public static class QcItemDto {
        private String  itemKey;          // 如 basic_0, fall_8
        private String  category;         // 维度名称
        private String  itemText;         // 条目完整文本
        private String  namesInputType;   // "free" | "multi"
        private Boolean checked;
        private Integer count;            // 质控人数
        private Integer issueCount;       // 质控问题人数
        private Integer naCount;          // 不适用人数
        private String  issueSummary;
        private String  rootAnalysis;     // 原因分析
        private String  remark;           // 备注
        private String  names;            // 被质控者，逗号分隔
    }

    /** 提交/更新日报请求 */
    @Data
    public static class ReportRequest {
        @NotBlank private String      reportDate;   // yyyy-MM-dd
        @NotBlank private String      qcPerson;
        private AdmDataDto            admData;
        private List<QcItemDto>       items;
    }

    /** 日报响应 */
    @Data
    public static class ReportResponse {
        private Long           id;
        private String         reportDate;
        private String         submitterId;
        private String         submitterName;
        private String         qcPerson;
        private AdmDataDto     admData;
        private List<QcItemDto> items;
        private String         createdAt;
        private String         lastEdit;
    }

    // ── Generic Result ────────────────────────────────────
    @Data
    public static class Result<T> {
        private int    code;
        private String message;
        private T      data;

        public static <T> Result<T> ok(T data) {
            Result<T> r = new Result<>();
            r.code = 200; r.message = "success"; r.data = data;
            return r;
        }
        public static <T> Result<T> ok() { return ok(null); }
        public static <T> Result<T> fail(int code, String msg) {
            Result<T> r = new Result<>();
            r.code = code; r.message = msg;
            return r;
        }
    }
}

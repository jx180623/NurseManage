-- ============================================================
-- 心内六科责班每日质控系统  数据库脚本  v2
-- MySQL 8.0+   与最新前端 HTML 完全对齐
-- ============================================================

CREATE DATABASE IF NOT EXISTS qc_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qc_system;

-- ① 护士人员表（支持独立密码）
CREATE TABLE IF NOT EXISTS nurse (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20)  NOT NULL UNIQUE COMMENT '工号（登录用，大写存储）',
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    password    VARCHAR(100) NOT NULL DEFAULT 'XNL226' COMMENT '个人登录密码，默认 XNL226',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB COMMENT='护士信息表';

-- ② 系统配置表（管理员密码等全局配置）
CREATE TABLE IF NOT EXISTS sys_config (
    config_key   VARCHAR(50)  NOT NULL PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    remark       VARCHAR(200),
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='系统配置表';

-- ③ 日报主表（每位护士每天一条，唯一约束）
CREATE TABLE IF NOT EXISTS day_report (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    report_date      DATE         NOT NULL COMMENT '质控日期',
    submitter_id     VARCHAR(20)  NOT NULL COMMENT '最后填报人工号',
    submitter_name   VARCHAR(50)  COMMENT '最后填报人姓名',
    qc_person        VARCHAR(50)  COMMENT '质控者姓名',
    -- 出入院维度（拆分存储，支持统计）
    adm_yesterday    INT          COMMENT '昨日患者总人数',
    adm_today        INT          COMMENT '今日患者总人数（自动计算）',
    adm_in           INT          COMMENT '入院人数',
    adm_transfer_in  INT          COMMENT '转入人数',
    adm_out          INT          COMMENT '出院人数',
    adm_transfer_out INT          COMMENT '转出人数',
    adm_er           INT          COMMENT '急诊转入人数',
    adm_surgery      INT          COMMENT '手术人数',
    adm_level1       INT          COMMENT '一级人数',
    adm_level4       INT          COMMENT '四级手术人数',
    adm_escort       INT          COMMENT '陪护人数',
    adm_rate         VARCHAR(20)  COMMENT '陪护率（今日人数为分母）',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_edit        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date (report_date),          -- 全科每天只有一条记录
    INDEX idx_report_date (report_date),
    INDEX idx_submitter   (submitter_id)
) ENGINE=InnoDB COMMENT='质控日报主表（全科每天唯一）';

-- ④ 质控条目明细表（完整 35 条）
--    item_key 格式：{catId}_{index}，与前端 CATS 定义一一对应
--    names_input_type: 'free'=手工输入, 'multi'=多选下拉
CREATE TABLE IF NOT EXISTS qc_item (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY,
    report_id        BIGINT        NOT NULL COMMENT '关联 day_report.id',
    category         VARCHAR(50)   NOT NULL COMMENT '维度名称',
    item_key         VARCHAR(30)   NOT NULL COMMENT '条目键，如 basic_0',
    item_text        VARCHAR(500)  NOT NULL COMMENT '条目完整文本',
    names_input_type VARCHAR(10)   NOT NULL DEFAULT 'multi' COMMENT 'free=手工填写 | multi=多选',
    checked          TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否完成',
    qc_count         INT           COMMENT '质控人数',
    issue_count      INT           COMMENT '质控问题人数',
    na_count         INT           COMMENT '不适用人数',
    issue_summary    TEXT          COMMENT '质控问题汇总',
    root_analysis    TEXT          COMMENT '原因分析',
    remark           TEXT          COMMENT '备注',
    nurses_involved  VARCHAR(1000) COMMENT '被质控者（逗号分隔；手工或多选均存此字段）',
    FOREIGN KEY (report_id) REFERENCES day_report(id) ON DELETE CASCADE,
    INDEX idx_report_id (report_id),
    INDEX idx_item_key  (item_key),
    INDEX idx_category  (category)
) ENGINE=InnoDB COMMENT='质控条目明细表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 系统配置：管理员密码
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('admin_password', 'admin888', '管理员密码，用于数据管理/统计/设置')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- 护士初始数据（24人，个人密码默认 XNL226）
INSERT INTO nurse (employee_id, name, password) VALUES
('N128','王雅榕','XNL226'),('N540','孙慧','XNL226'),('N245','陈婷1','XNL226'),
('N403','邱燕萍','XNL226'),('E008','黄红燕','XNL226'),('N341','林妙玲','XNL226'),
('N927','傅月青','XNL226'),('N267','黄小婕','XNL226'),('N193','游颜竹','XNL226'),
('9006','黄怡','XNL226'),('N646','郭姝君','XNL226'),('N364','吴梅','XNL226'),
('N181','庄思思','XNL226'),('N385','唐媛玲','XNL226'),('N238','黄燕慧','XNL226'),
('N185','杨菲','XNL226'),('N902','张萍萍','XNL226'),('N284','赵丽莉','XNL226'),
('N455','陈春娜','XNL226'),('N440','詹暖虹','XNL226'),('N447','苏丽雯','XNL226'),
('N462','杨佳雯','XNL226'),('N923','蔡真真','XNL226'),('N226','聂伟琳','XNL226')
ON DUPLICATE KEY UPDATE name = VALUES(name);

package com.xnl.qc.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "qc_item")
public class QcItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private DayReport report;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "item_key", nullable = false, length = 30)
    private String itemKey;

    @Column(name = "item_text", nullable = false, length = 500)
    private String itemText;

    @Column(name = "names_input_type", nullable = false, length = 10)
    private String namesInputType = "multi";

    @Column(name = "checked", nullable = false)
    private Boolean checked = false;

    @Column(name = "qc_count")
    private Integer qcCount;

    @Column(name = "issue_count")
    private Integer issueCount;

    @Column(name = "na_count")
    private Integer naCount;

    @Column(name = "issue_summary", columnDefinition = "TEXT")
    private String issueSummary;

    /** 原因分析 */
    @Column(name = "root_analysis", columnDefinition = "TEXT")
    private String rootAnalysis;

    /** 备注 */
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "nurses_involved", length = 1000)
    private String nursesInvolved;
}

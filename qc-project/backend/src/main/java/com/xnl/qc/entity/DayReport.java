package com.xnl.qc.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "day_report",
       uniqueConstraints = @UniqueConstraint(columnNames = {"report_date"}))
public class DayReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "submitter_id", nullable = false, length = 20)
    private String submitterId;

    @Column(name = "submitter_name", length = 50)
    private String submitterName;

    @Column(name = "qc_person", length = 50)
    private String qcPerson;

    // ── 出入院维度（与前端 ADM 字段对应）──
    @Column(name = "adm_yesterday") private Integer admYesterday;  // 昨日患者总人数
    @Column(name = "adm_today")     private Integer admToday;      // 今日患者总人数（自动计算）
    @Column(name = "adm_in")        private Integer admIn;
    @Column(name = "adm_transfer_in")  private Integer admTransferIn;
    @Column(name = "adm_out")       private Integer admOut;
    @Column(name = "adm_transfer_out") private Integer admTransferOut;
    @Column(name = "adm_er")        private Integer admEr;
    @Column(name = "adm_surgery")   private Integer admSurgery;
    @Column(name = "adm_level1")    private Integer admLevel1;
    @Column(name = "adm_level4")    private Integer admLevel4;
    @Column(name = "adm_escort")    private Integer admEscort;
    @Column(name = "adm_rate", length = 20) private String admRate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_edit")
    private LocalDateTime lastEdit;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QcItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        lastEdit  = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        lastEdit = LocalDateTime.now();
    }
}

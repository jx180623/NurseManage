package com.xnl.qc.repository;

import com.xnl.qc.entity.DayReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayReportRepository extends JpaRepository<DayReport, Long> {

    /** 全科唯一：按日期查（不再区分填报人） */
    Optional<DayReport> findByReportDate(LocalDate date);

    /** 全科历史：按日期倒序全部返回 */
    List<DayReport> findAllByOrderByReportDateDesc();

    List<DayReport> findByReportDateBetweenOrderByReportDateDesc(LocalDate start, LocalDate end);

    @Query("SELECT d FROM DayReport d LEFT JOIN FETCH d.items WHERE d.id = :id")
    Optional<DayReport> findByIdWithItems(@Param("id") Long id);

    /** 全科历史（含 items），按日期倒序 */
    @Query("SELECT d FROM DayReport d LEFT JOIN FETCH d.items ORDER BY d.reportDate DESC")
    List<DayReport> findAllWithItems();
}

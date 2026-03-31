package com.xnl.qc.repository;

import com.xnl.qc.entity.Nurse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NurseRepository extends JpaRepository<Nurse, Long> {
    Optional<Nurse> findByEmployeeIdIgnoreCase(String employeeId);
    List<Nurse>     findByEnabledTrueOrderByEmployeeId();
    boolean         existsByEmployeeIdIgnoreCase(String employeeId);
}

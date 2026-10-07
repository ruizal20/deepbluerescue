package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long > {

    // Consulta A
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Consulta B
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Consulta C
    List<RescueCase> findByRescueCenterCode(String code);

    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(
        LocalDate date);
}
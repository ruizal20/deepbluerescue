package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {
  @Query("""
      SELECT DISTINCT s
      FROM Specialist s
      JOIN s.expertiseAreas e
      WHERE s.active = true
        AND LOWER(e.name) = LOWER(:expertiseName)
      ORDER BY s.lastName ASC, s.firstName ASC
      """)
  List<Specialist> findActiveByExpertise(@Param("expertiseName") String expertiseName);
  Optional<Specialist> findByProfessionalCode(String professionalCode);
}
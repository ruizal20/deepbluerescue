package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(long animalId);
    List<Treatment> findByAnimalAnimalCode(String animalCode);

    @Query("""
            SELECT t
            FROM Treatment t
            WHERE t.performedAt BETWEEN :start AND :end
            ORDER BY t.performedAt ASC
            """)
    List<Treatment> findTreatmentsBetweenDates(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT t
            FROM Treatment t
            JOIN t.animal a
            JOIN a.rescueCase rc
            JOIN rc.rescueCenter center
            WHERE center.code = :centerCode
            """)
    List<Treatment> findByRescueCenterCode(@Param("centerCode") String centerCode);

    @Query("""
    SELECT DISTINCT t 
    FROM Treatment t 
    JOIN t.specialist s 
    JOIN s.expertiseAreas e 
    WHERE e.name = :expertiseName
    """)
List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);
}
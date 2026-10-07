package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);
    List<Animal> findByRescueCaseStatus(RescueStatus status);
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);


    //reto sin guia
 @Query("SELECT DISTINCT a FROM Animal a " +
       "JOIN a.rescueCase c " +
       "JOIN a.treatments t " +
       "JOIN t.specialist s " +
       "JOIN s.expertiseAreas e " +
       "WHERE c.status = :status " +
       "AND LOWER(e.name) = LOWER(:expertiseName)")
List<Animal> findAnimalsByStatusAndSpecialistExpertise(
    @Param("status") RescueStatus status, 
    @Param("expertiseName") String expertiseName
);

}
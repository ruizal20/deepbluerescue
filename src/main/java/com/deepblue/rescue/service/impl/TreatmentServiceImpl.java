package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(
            AnimalRepository animalRepository,
            SpecialistRepository specialistRepository,
            TreatmentRepository treatmentRepository,
            TreatmentMapper mapper) {
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

        @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        return treatmentRepository
                .findByAnimalAnimalCode(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public TreatmentResponse register(CreateTreatmentRequest request) {
        // 1 y 2: Buscar Animal y Especialista (Te sugiero usar ResourceNotFoundException como pide el lab)
        Animal animal = animalRepository.findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException("Animal no encontrado"));

        Specialist specialist = specialistRepository.findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException("Especialista no encontrado"));

        // Regla 3: Validar que el especialista esté activo
        if (!specialist.isActive()) {
            throw new BusinessRuleException("El especialista no está activo");
        }

        // 4. Obtener RescueCase del Animal
        RescueCase rescueCase = animal.getRescueCase();

        // Regla 4: Validar status (no puede ser RELEASED)
        if (rescueCase.getStatus() == RescueStatus.RELEASED) {
            throw new BusinessRuleException("El caso ya fue liberado");
        }

        // Regla 5: Validar que la fecha del tratamiento no sea anterior al rescate
        if (request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException("La fecha del tratamiento no puede ser anterior a la fecha de rescate");
        }

        // Crear, guardar y mapear (esto ya lo tenías excelente, solo adapté el constructor al sugerido en el lab)
        Treatment treatment = new Treatment();
        treatment.setAnimal(animal);
        treatment.setSpecialist(specialist);
        treatment.setPerformedAt(request.performedAt());
        treatment.setType(request.type());
        treatment.setDescription(request.description());

        Treatment savedTreatment = treatmentRepository.save(treatment);

        return mapper.toResponse(savedTreatment);
 

    }

}
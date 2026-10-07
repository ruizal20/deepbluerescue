package com.deepblue.rescue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service; 

    @Test
    void shouldRegisterTreatmentSuccessfully() {

        Animal animal = mock(Animal.class);
        Specialist specialist = mock(Specialist.class);
        RescueCase rescueCase = mock(RescueCase.class);
        Treatment treatment = mock(Treatment.class);
        TreatmentResponse response = mock(TreatmentResponse.class);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning of injury");

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        when(specialist.isActive())
                .thenReturn(true);

        when(animal.getRescueCase())
                .thenReturn(rescueCase);

        when(rescueCase.getStatus())
                .thenReturn(RescueStatus.IN_REHABILITATION);

        when(rescueCase.getRescueDate())
                .thenReturn(LocalDate.of(2026, 8, 20));

        when(treatmentRepository.save(any(Treatment.class)))
                .thenReturn(treatment);

        when(mapper.toResponse(treatment))
                .thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result)
                .isEqualTo(response);

        verify(treatmentRepository)
                .save(any(Treatment.class));

        verify(mapper)
                .toResponse(treatment);
    }

    
}

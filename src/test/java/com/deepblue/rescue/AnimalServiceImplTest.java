package com.deepblue.rescue;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldReturnTrueWhenAnimalCanReceiveTreatment() {
        Animal animal = mock(Animal.class);
        RescueCase rescueCase = mock(RescueCase.class);
        
        when(repository.findByAnimalCode("AN-100")).thenReturn(Optional.of(animal));
        when(animal.getRescueCase()).thenReturn(rescueCase);
        when(rescueCase.getStatus()).thenReturn(RescueStatus.IN_REHABILITATION);

        boolean result = service.canReceiveTreatment("AN-100");

        assertThat(result).isTrue();
    }
}

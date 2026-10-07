package com.deepblue.rescue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    @Test
void shouldFindRescueCaseByCode() {

    RescueCase rescueCase = new RescueCase();

    // ✅ CORRECTO: debes pasar todos los campos requeridos
    RescueCaseResponse response = new RescueCaseResponse(
    1L,                        // Long id
    "RES-001",                 // String caseCode
    LocalDate.now(),           // LocalDate rescueDate
    "Centro de Rescate A",     // String rescueCenter
    RescueStatus.RELEASED,       // RescueStatus status
    "Ubicación",               // String rescueLocation
    "Detalles adicionales"     // String details
);

    when(
        repository.findByCaseCode("RES-001")
    ).thenReturn(
        Optional.of(rescueCase)
    );

    when(
        mapper.toResponse(rescueCase)
    ).thenReturn(response);

    RescueCaseResponse result = service.findByCode("RES-001");

    assertThat(result).isEqualTo(response);

    verify(repository).findByCaseCode("RES-001");

    verify(mapper).toResponse(rescueCase);
}

@Test
void shouldThrowExceptionWhenRescueCaseDoesNotExist() {

    when(repository.findByCaseCode("RES-999"))
            .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            service.findByCode("RES-999")
    )
            .isInstanceOf(ResourceNotFoundException.class);

    verify(repository)
            .findByCaseCode("RES-999");

    verify(mapper, never())
            .toResponse(any(RescueCase.class));
}

//TEST 30

@Test
void shouldChangeStatusWhenTransitionIsValid() {

    RescueCase rescueCase = mock(RescueCase.class);

    RescueCaseResponse response = mock(RescueCaseResponse.class);

    when(repository.findByCaseCode("RES-001"))
            .thenReturn(Optional.of(rescueCase));

    when(rescueCase.getStatus())
            .thenReturn(RescueStatus.ADMITTED);

    when(repository.save(rescueCase))
            .thenReturn(rescueCase);

    when(mapper.toResponse(rescueCase))
            .thenReturn(response);

    ChangeRescueStatusRequest request =
            new ChangeRescueStatusRequest(
                    RescueStatus.UNDER_EVALUATION
            );

    RescueCaseResponse result =
            service.changeStatus("RES-001", request);

    assertThat(result)
            .isEqualTo(response);

    verify(rescueCase)
            .setStatus(RescueStatus.UNDER_EVALUATION);

    verify(repository)
            .save(rescueCase);

    verify(mapper)
            .toResponse(rescueCase);
}

//TEST 31

@Test
void shouldThrowExceptionWhenTransitionIsInvalid() {

    RescueCase rescueCase = mock(RescueCase.class);

    when(repository.findByCaseCode("RES-001"))
            .thenReturn(Optional.of(rescueCase));

    when(rescueCase.getStatus())
            .thenReturn(RescueStatus.ADMITTED);

    ChangeRescueStatusRequest request =
            new ChangeRescueStatusRequest(
                    RescueStatus.READY_FOR_RELEASE
            );

    assertThatThrownBy(() ->
            service.changeStatus("RES-001", request)
    )
            .isInstanceOf(BusinessRuleException.class);

    verify(repository)
            .findByCaseCode("RES-001");

    verify(repository, never())
            .save(any());
}

}


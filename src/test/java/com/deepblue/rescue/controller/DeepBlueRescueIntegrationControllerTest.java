package com.deepblue.rescue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.AnimalService;
import com.deepblue.rescue.service.RescueCaseService;
import com.deepblue.rescue.service.TreatmentService;

@WebMvcTest({
        RescueCaseController.class,
        AnimalController.class,
        TreatmentController.class
})
@Import(GlobalExceptionHandler.class)
class DeepBlueRescueIntegrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService rescueCaseService;

    @MockitoBean
    private AnimalService animalService;

    @MockitoBean
    private TreatmentService treatmentService;

    // ============================================================
    // OPERACIÓN 1 - GET /api/rescue-cases/RES-2026-100
    // ============================================================

    @Test
    void operation84_shouldReturnRescueCase() throws Exception {

        RescueCaseResponse response = new RescueCaseResponse(
                100L,
                "RES-2026-100",
                LocalDate.of(2026, 8, 20),
                "Bahía Concha",
                RescueStatus.IN_REHABILITATION,
                "SPEC-001",
                "AN-2026-100"
        );

        when(rescueCaseService.findByCode("RES-2026-100"))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/rescue-cases/{code}", "RES-2026-100")
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.caseCode")
                                .value("RES-2026-100")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("IN_REHABILITATION")
                )
                .andExpect(
                        jsonPath("$.animalCode")
                                .value("AN-2026-100")
                );

        verify(rescueCaseService)
                .findByCode("RES-2026-100");
    }

    // ============================================================
    // OPERACIÓN 2 - GET /api/animals/AN-2026-100
    // ============================================================

    
    @Test
void operation85_shouldReturnAnimal() throws Exception {

    AnimalResponse response = new AnimalResponse(
            100L,
            "AN-2026-100",
            "Green Sea Turtle",
            "Chelonia mydas",
            "FEMALE",
            "RES-2026-100",
            RescueStatus.IN_REHABILITATION
    );

    when(animalService.findByCode("AN-2026-100"))
            .thenReturn(response);

    mockMvc.perform(
            get("/api/animals/{animalCode}", "AN-2026-100")
    )
            .andExpect(status().isOk())
            .andExpect(
                    jsonPath("$.animalCode")
                            .value("AN-2026-100")
            )
            .andExpect(
                    jsonPath("$.commonName")
                            .value("Green Sea Turtle")
            )
            .andExpect(
                    jsonPath("$.scientificName")
                            .value("Chelonia mydas")
            )
            .andExpect(
                    jsonPath("$.sex")
                            .value("FEMALE")
            );

    verify(animalService)
            .findByCode("AN-2026-100");
}

    // ============================================================
    // OPERACIÓN 4 - PATCH status
    // IN_REHABILITATION -> READY_FOR_RELEASE
    // ============================================================

    @Test
    void operation87_shouldChangeStatusToReadyForRelease()
            throws Exception {

        RescueCaseResponse response = new RescueCaseResponse(
                100L,
                "RES-2026-100",
                LocalDate.of(2026, 8, 20),
                "Bahía Concha",
                RescueStatus.READY_FOR_RELEASE,
                "SPEC-001",
                "AN-2026-100"
        );

        when(rescueCaseService.changeStatus(
                eq("RES-2026-100"),
                any(ChangeRescueStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                patch(
                        "/api/rescue-cases/{code}/status",
                        "RES-2026-100"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "READY_FOR_RELEASE"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.caseCode")
                                .value("RES-2026-100")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("READY_FOR_RELEASE")
                );

        verify(rescueCaseService)
                .changeStatus(
                        eq("RES-2026-100"),
                        any(ChangeRescueStatusRequest.class)
                );
    }

    // ============================================================
    // OPERACIÓN 5 - PATCH status = null
    // Resultado: 400 Bad Request
    // ============================================================

    @Test
    void operation88_shouldReturn400WhenStatusIsNull()
            throws Exception {

        mockMvc.perform(
                patch(
                        "/api/rescue-cases/{code}/status",
                        "RES-2026-100"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": null
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Request validation failed")
                )
                .andExpect(
                        jsonPath("$.details.status")
                                .value("Status is required")
                );

        verify(
                rescueCaseService,
                never()
        ).changeStatus(
                anyString(),
                any()
        );
    }

    // ============================================================
    // OPERACIÓN 6 - Transición inválida
    // Resultado: 409 Conflict
    // ============================================================

    @Test
    void operation89_shouldReturn409WhenTransitionIsInvalid()
            throws Exception {

        when(rescueCaseService.changeStatus(
                eq("RES-2026-100"),
                any(ChangeRescueStatusRequest.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "Invalid status transition"
                        )
                );

        mockMvc.perform(
                patch(
                        "/api/rescue-cases/{code}/status",
                        "RES-2026-100"
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "IN_REHABILITATION"
                                }
                                """)
        )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Invalid status transition")
                )
                .andExpect(
                        jsonPath("$.details")
                                .isMap()
                );
    }

    // ============================================================
    // OPERACIÓN 7 - GET /api/animals/AN-999
    // Resultado: 404 Not Found
    // ============================================================

    @Test
    void operation90_shouldReturn404WhenAnimalDoesNotExist()
            throws Exception {

        when(animalService.findByCode("AN-999"))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Animal not found: AN-999"
                        )
                );

        mockMvc.perform(
                get("/api/animals/{animalCode}", "AN-999")
        )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.timestamp")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Animal not found: AN-999")
                )
                .andExpect(
                        jsonPath("$.details")
                                .isMap()
                );

        verify(animalService)
                .findByCode("AN-999");
    }
}
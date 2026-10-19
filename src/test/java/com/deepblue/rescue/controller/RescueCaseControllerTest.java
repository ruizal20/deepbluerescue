package com.deepblue.rescue.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
public class RescueCaseControllerTest {

        @MockitoBean
        private RescueCaseService service;

        @Autowired
        private MockMvc mockMvc;

        @Test
        void shouldReturnRescueCaseByCode() throws Exception {

                RescueCaseResponse response = new RescueCaseResponse(
                                1L,
                                "RES-2026-001",
                                LocalDate.of(2026, 8, 20),
                                "Bahia Concha",
                                RescueStatus.IN_REHABILITATION,
                                "DB-CAR",
                                "AN-2026-001");

                when(service.findByCode("RES-2026-001"))
                                .thenReturn(response);

                mockMvc.perform(
                                get("/api/rescue-cases/{code}", "RES-2026-001"))
                                .andExpect(status().isOk())
                                .andExpect(
                                                jsonPath("$.caseCode")
                                                                .value("RES-2026-001"))
                                .andExpect(
                                                jsonPath("$.status")
                                                                .value("IN_REHABILITATION"));

                verify(service)
                                .findByCode("RES-2026-001");
        }

        @Test
        void shouldReturn404WhenCaseDoesNotExist()
                        throws Exception {
                when(service.findByCode("RES-999"))
                                .thenThrow(new ResourceNotFoundException("Rescue case not found: RES-999"));
                mockMvc.perform(
                                get("/api/rescue-cases/{code}", "RES-999"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.timestamp").exists())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("Rescue case not found: RES-999"))
                                .andExpect(jsonPath("$.details").isMap());
        }

        @Test
        void shouldReturnCasesByStatus() throws Exception {
                // 1. Preparar datos de respuesta simulados
                RescueCaseResponse case1 = new RescueCaseResponse(
                                1L,
                                "RES-2026-001",
                                LocalDate.of(2026, 8, 20),
                                "Bahía Concha",
                                RescueStatus.IN_REHABILITATION,
                                "DB-CAR",
                                "AN-2026-001");

                RescueCaseResponse case2 = new RescueCaseResponse(
                                2L,
                                "RES-2026-002",
                                LocalDate.of(2026, 8, 21),
                                "Taganga",
                                RescueStatus.IN_REHABILITATION,
                                "DB-CAR",
                                "AN-2026-002");

                // 2. Configurar el mock del servicio
                when(
                                service.findByStatus(
                                                RescueStatus.IN_REHABILITATION))
                                .thenReturn(
                                                List.of(case1, case2));

                // 3. Ejecutar la petición GET con el Query Parameter y verificar el resultado
                mockMvc.perform(
                                get("/api/rescue-cases")
                                                .param("status", "IN_REHABILITATION"))
                                .andExpect(
                                                status().isOk() // Valida 200 OK
                                )
                                .andExpect(
                                                jsonPath("$.length()").value(2) // Valida que retorna 2 elementos[cite:
                                                                                // 34, 35]
                                )
                                .andExpect(
                                                jsonPath("$[0].status").value("IN_REHABILITATION"))
                                .andExpect(
                                                jsonPath("$[1].status").value("IN_REHABILITATION"));

                // 4. Verificar que el servicio fue efectivamente llamado
                verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
        }

        @Test
        void shouldReturn400WhenStatusIsInvalid() throws Exception {
                mockMvc.perform(
                                get("/api/rescue-cases")
                                                .param("status", "FLYING") // Valor inválido que no pertenece al enum
                                                                           // RescueStatus
                )
                                .andExpect(
                                                status().isBadRequest() // Valida el código HTTP 400
                                )
                                .andExpect(
                                                jsonPath("$.status").value(400))
                                .andExpect(
                                                jsonPath("$.error").value("Bad Request"))
                                .andExpect(
                                                jsonPath("$.message").value("Invalid request parameter"))
                                .andExpect(
                                                jsonPath("$.details.status").exists() // Verifica que detalle el
                                                                                      // parámetro fallido
                                );
        }

        @Test
        void shouldChangeRescueCaseStatus() throws Exception {
                // 1. Preparar la respuesta simulada esperada tras el cambio de estado
                RescueCaseResponse response = new RescueCaseResponse(
                                1L,
                                "RES-001",
                                LocalDate.of(2026, 8, 20),
                                "Bahía Concha",
                                RescueStatus.READY_FOR_RELEASE,
                                "DB-CAR",
                                "AN-2026-001");

                // 2. Configurar el mock del servicio usando ArgumentMatchers (eq y any)
                when(
                                service.changeStatus(
                                                eq("RES-001"),
                                                any(ChangeRescueStatusRequest.class)))
                                .thenReturn(response);

                // 3. Ejecutar la petición PATCH enviando el JSON con el nuevo estado
                mockMvc.perform(
                                patch("/api/rescue-cases/{code}/status", "RES-001")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "status": "READY_FOR_RELEASE"
                                                                }
                                                                """))
                                .andExpect(
                                                status().isOk() // Valida el código HTTP 200 OK
                                )
                                .andExpect(
                                                jsonPath("$.status").value("READY_FOR_RELEASE") // Valida que el estado
                                                                                                // retornado cambió
                                );

                // 4. Verificar que el servicio fue invocado correctamente con los parámetros
                // esperados
                verify(service).changeStatus(
                                eq("RES-001"),
                                any(ChangeRescueStatusRequest.class));
        }

        @Test
        void shouldReturn400WhenPatchRequestIsInvalid() throws Exception {

                mockMvc.perform(
                                patch("/api/rescue-cases/{code}/status", "RES-001")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.timestamp").exists())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(
                                                jsonPath("$.message")
                                                                .value("Request validation failed"))
                                .andExpect(
                                                jsonPath("$.details.status")
                                                                .value("Status is required"));

                verify(service, never())
                                .changeStatus(
                                                anyString(),
                                                any());
        }

        @Test
    void shouldReturn409WhenBusinessRuleIsViolated() throws Exception {
        when(
                service.changeStatus(
                        eq("RES-001"),
                        any(ChangeRescueStatusRequest.class)
                )
        ).thenThrow(
                new BusinessRuleException("Invalid status transition")
        );

        mockMvc.perform(
                        patch("/api/rescue-cases/{code}/status", "RES-001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "status": "READY_FOR_RELEASE"
                                        }
                                        """)
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status").value(409)
                )
                .andExpect(
                        jsonPath("$.error").value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message").value("Invalid status transition")
                )
                .andExpect(
                        jsonPath("$.details").isMap()
                );
    }

    

}
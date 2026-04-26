package com.example.conges.controller;

import com.example.conges.config.SecurityConfig;
import com.example.conges.dto.HistoryResponse;
import com.example.conges.entity.History;
import com.example.conges.entity.History.ActionType;
import com.example.conges.entity.UserEntity;
import com.example.conges.mapper.HistoryMapper;
import com.example.conges.entity.Role;
import com.example.conges.repository.UserRepository;
import com.example.conges.service.HistoryService;
import com.example.conges.service.JwtService;
import com.example.conges.service.export.ExcelExportService;
import com.example.conges.service.export.PdfExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests pour HistoryController
 */
@WebMvcTest(HistoryController.class)
@Import(SecurityConfig.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HistoryService historyService;

    @MockBean
    private PdfExportService pdfExportService;

    @MockBean
    private ExcelExportService excelExportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private HistoryMapper historyMapper;

    @Test
    @WithMockUser(roles = "RH")
    void testGetHistory() throws Exception {
        // Arrangé
        UserEntity user = UserEntity.builder()
                .id(1L)
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@example.com")
                .role(Role.RH)
                .build();

        History history = History.builder()
                .id(1L)
                .user(user)
                .actionType(ActionType.CREATE)
                .description("Demande créée")
                .actionDate(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        when(historyService.getHistory(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(history), pageable, 1));
        when(historyMapper.toResponse(any(History.class)))
                .thenReturn(HistoryResponse.builder()
                        .id(1L)
                        .actionType(ActionType.CREATE)
                        .description("Demande créée")
                        .build());

        // Act & Assert
        mockMvc.perform(get("/api/history")
                .param("page", "0")
                .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].actionType").value("CREATE"));
    }

    @Test
    @WithMockUser(roles = "RH")
    void testGetStatistics() throws Exception {
        // Arrangé
        Map<ActionType, Long> stats = Map.of(
                ActionType.CREATE, 145L,
                ActionType.APPROVE, 120L,
                ActionType.REJECT, 15L
        );

        when(historyService.getActionStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/history/statistics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CREATE").value(145))
                .andExpect(jsonPath("$.APPROVE").value(120))
                .andExpect(jsonPath("$.REJECT").value(15));
    }

    @Test
    @WithMockUser(roles = "RH")
    void testExportHistoryPdf() throws Exception {
        // Arrangé
        List<History> historyList = List.of();
        byte[] pdfBytes = "Mock PDF content".getBytes();

        when(historyService.getHistoryForExport(any(), any(), any(), any(), any(), any()))
                .thenReturn(historyList);
        when(pdfExportService.generateHistoryReport(anyList(), any()))
                .thenReturn(pdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/history/export/pdf"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RH")
    void testExportHistoryExcel() throws Exception {
        // Arrangé
        List<History> historyList = List.of();
        byte[] excelBytes = "Mock Excel content".getBytes();

        when(historyService.getHistoryForExport(any(), any(), any(), any(), any(), any()))
                .thenReturn(historyList);
        when(excelExportService.generateHistoryExcel(anyList(), any()))
                .thenReturn(excelBytes);

        // Act & Assert
        mockMvc.perform(get("/api/history/export/excel"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetHistoryUnauthorized() throws Exception {
        // Act & Assert - L'employé n'a pas accès
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isForbidden());
    }
}

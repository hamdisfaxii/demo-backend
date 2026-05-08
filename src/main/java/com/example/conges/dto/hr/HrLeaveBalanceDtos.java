package com.example.conges.dto.hr;

import com.example.conges.entity.Role;
import com.example.conges.entity.TypeConge;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class HrLeaveBalanceDtos {

    private HrLeaveBalanceDtos() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String nom;
        private String prenom;
        private String pays;
        private String departement;
        private Role role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceLine {
        private TypeConge typeConge;
        private double total;
        private double used;
        private double remaining;
        private boolean readOnly;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceRow {
        private UserInfo user;
        private Integer year;
        private List<BalanceLine> balances;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResponse {
        private List<BalanceRow> items;
        private int page;
        private int size;
        private long total;
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLine {
        @NotNull
        private TypeConge typeConge;

        /** Nouvelle valeur du solde restant (jours). */
        @NotNull
        @Min(0)
        private Double remaining;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotNull
        private Long userId;

        @NotNull
        private Integer year;

        @NotNull
        @Valid
        private List<UpdateLine> updates;
    }
}


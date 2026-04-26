package com.example.conges.dto.config;

import javax.validation.constraints.Min;
import lombok.Data;

@Data
public class LeaveTypeConfigRequest {
    private String libelle;
    private String description;
    private String couleur;
    private Boolean active;
    private Boolean requiresApproval;
    @Min(0)
    private Long delai;
}

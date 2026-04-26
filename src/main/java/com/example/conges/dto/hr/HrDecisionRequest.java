package com.example.conges.dto.hr;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HrDecisionRequest {

    @NotBlank
    private String action; // APPROVE | REJECT

    private String comment;
}

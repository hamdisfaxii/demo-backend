package com.example.conges.dto.workflow;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class WorkflowConfigRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String countryCode;
    @NotEmpty
    @Valid
    private List<WorkflowStepConfigRequest> steps;
}

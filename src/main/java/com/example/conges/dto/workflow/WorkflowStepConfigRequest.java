package com.example.conges.dto.workflow;

import com.example.conges.entity.Role;
import com.example.conges.entity.TypeConge;
import com.example.conges.entity.WorkflowStepType;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowStepConfigRequest {
    @NotNull
    private Integer stepOrder;
    @NotNull
    private WorkflowStepType stepType;
    @NotNull
    private Role approverRole;
    private Boolean required = Boolean.TRUE;
    private Integer minDays;
    private Integer maxDays;
    private Set<TypeConge> applicableLeaveTypes;
}

package com.example.conges.dto;

import com.example.conges.entity.StatutConge;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesRhResponse {

    private Map<StatutConge, Long> nombreParStatut;
}

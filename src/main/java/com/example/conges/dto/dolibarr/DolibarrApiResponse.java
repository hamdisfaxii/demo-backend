package com.example.conges.dto.dolibarr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour réponse API lors de la création d'un utilisateur Dolibarr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DolibarrApiResponse {

    private Long id;
    private String login;
    private String email;
    private String firstname;
    private String lastname;
    private Boolean success;
    private String message;
    private String error;
}

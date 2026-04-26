package com.example.conges.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO pour les notifications par email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDto {

    private String to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String templateName;
    
    @Builder.Default
    private Map<String, Object> variables = new HashMap<>();

    /**
     * Ajoute une variable au contexte du template
     */
    public EmailNotificationDto addVariable(String key, Object value) {
        if (this.variables == null) {
            this.variables = new HashMap<>();
        }
        this.variables.put(key, value);
        return this;
    }

    /**
     * Ajoute un destinataire en copie
     */
    public EmailNotificationDto addCc(String email) {
        if (this.cc == null) {
            this.cc = new ArrayList<>();
        }
        this.cc.add(email);
        return this;
    }

    /**
     * Ajoute un destinataire en copie cachée
     */
    public EmailNotificationDto addBcc(String email) {
        if (this.bcc == null) {
            this.bcc = new ArrayList<>();
        }
        this.bcc.add(email);
        return this;
    }
}

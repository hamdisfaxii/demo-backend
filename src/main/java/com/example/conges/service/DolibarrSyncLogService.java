package com.example.conges.service;

import com.example.conges.entity.DolibarrSyncLog;
import com.example.conges.entity.SyncDirection;
import com.example.conges.entity.SyncStatus;
import com.example.conges.repository.DolibarrSyncLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DolibarrSyncLogService {

    private final DolibarrSyncLogRepository dolibarrSyncLogRepository;
    private final ObjectMapper objectMapper;

    public void logSuccess(
            String entityType,
            String operation,
            Long localEntityId,
            Long remoteEntityId,
            SyncDirection direction,
            Object payload) {
        saveLog(entityType, operation, localEntityId, remoteEntityId, direction, SyncStatus.SUCCESS, null, payload);
    }

    public void logFailure(
            String entityType,
            String operation,
            Long localEntityId,
            Long remoteEntityId,
            SyncDirection direction,
            String message,
            Object payload) {
        saveLog(entityType, operation, localEntityId, remoteEntityId, direction, SyncStatus.FAILED, message, payload);
    }

    private void saveLog(
            String entityType,
            String operation,
            Long localEntityId,
            Long remoteEntityId,
            SyncDirection direction,
            SyncStatus status,
            String message,
            Object payload) {
        try {
            DolibarrSyncLog logEntry = DolibarrSyncLog.builder()
                    .entityType(entityType)
                    .operation(operation)
                    .localEntityId(localEntityId)
                    .remoteEntityId(remoteEntityId)
                    .direction(direction)
                    .status(status)
                    .message(message)
                    .payload(serializePayload(payload))
                    .build();
            dolibarrSyncLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Impossible d'enregistrer le log de synchronisation Dolibarr", ex);
        }
    }

    private String serializePayload(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.toString();
        }
    }
}

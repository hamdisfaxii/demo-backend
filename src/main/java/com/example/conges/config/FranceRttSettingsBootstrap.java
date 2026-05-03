package com.example.conges.config;

import com.example.conges.entity.FranceRttAccrualMode;
import com.example.conges.entity.FranceRttSettings;
import com.example.conges.repository.FranceRttSettingsRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FranceRttSettingsBootstrap implements ApplicationRunner {

    private static final long ID = 1L;
    private final FranceRttSettingsRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.existsById(ID)) {
            return;
        }
        repository.save(FranceRttSettings.builder()
                .id(ID)
                .accrualMode(FranceRttAccrualMode.CONTRACT_HOURS)
                .adminOverrideDays(null)
                .updatedAt(LocalDateTime.now())
                .build());
    }
}

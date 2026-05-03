package com.example.conges.service;

import com.example.conges.entity.FranceRttAccrualMode;
import com.example.conges.entity.UserEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FranceRttComputationService {

    private static final int SCALE = 2;

    /** Heures défaut si non renseignées (référence 39 h). */
    public static BigDecimal resolvedWeeklyHours(UserEntity user) {
        if (user.getWeeklyHours() == null) {
            return bd(39);
        }
        return user.getWeeklyHours().setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Jours RTT annuels pleins (avant prorata embauche) selon le barème légal simplifié + heures.
     */
    public BigDecimal fullYearEntitlementFromWeeklyHours(BigDecimal weeklyHours) {
        if (weeklyHours == null) {
            weeklyHours = bd(39);
        }
        double h = weeklyHours.doubleValue();
        if (h <= 35.0 + 1e-9) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        if (h <= 36.0 + 1e-9) {
            return bd(4);
        }
        if (h <= 37.0 + 1e-9) {
            return bd(6);
        }
        if (h <= 38.0 + 1e-9) {
            return bd(8);
        }
        return bd(10);
    }

    public LocalDate effectiveHireDate(UserEntity user) {
        if (user.getHireDate() != null) {
            return user.getHireDate();
        }
        if (user.getCreatedAt() != null) {
            return user.getCreatedAt().toLocalDate();
        }
        return LocalDate.now();
    }

    /** Mois calendaires inclus entre début d'emploi dans l'année et fin d'année. */
    public int monthsEmployedInCalendarYear(UserEntity user, int year) {
        LocalDate jan1 = LocalDate.of(year, 1, 1);
        LocalDate dec31 = LocalDate.of(year, 12, 31);
        LocalDate hire = effectiveHireDate(user);
        LocalDate effectiveStart = hire.isAfter(jan1) ? hire : jan1;
        if (effectiveStart.isAfter(dec31)) {
            return 0;
        }
        return monthsBetweenInclusive(YearMonth.from(effectiveStart), YearMonth.from(dec31));
    }

    /**
     * Mois écoulés depuis le début d'emploi dans l'année jusqu'à la date de référence (inclus).
     */
    public int monthsElapsedEmployedInYear(UserEntity user, int year, LocalDate asOf) {
        LocalDate jan1 = LocalDate.of(year, 1, 1);
        LocalDate dec31 = LocalDate.of(year, 12, 31);
        LocalDate hire = effectiveHireDate(user);
        LocalDate effectiveStart = hire.isAfter(jan1) ? hire : jan1;
        LocalDate capped = asOf.isAfter(dec31) ? dec31 : asOf;
        if (capped.isBefore(effectiveStart)) {
            return 0;
        }
        return monthsBetweenInclusive(YearMonth.from(effectiveStart), YearMonth.from(capped));
    }

    private static int monthsBetweenInclusive(YearMonth start, YearMonth end) {
        return (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue()) + 1;
    }

    /** Quota annualisé proratisé embauche (sans mode d'acquisition mensuelle). */
    public BigDecimal proratedAnnualTotal(BigDecimal fullYearDays, UserEntity user, int year) {
        int months = monthsEmployedInCalendarYear(user, year);
        if (months <= 0 || fullYearDays.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return fullYearDays
                .multiply(bd(months))
                .divide(bd(12), SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal earnedCapYtd(
            FranceRttAccrualMode mode,
            BigDecimal proratedRowTotal,
            UserEntity user,
            int year,
            LocalDate asOf
    ) {
        if (proratedRowTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        LocalDate jan1 = LocalDate.of(year, 1, 1);
        LocalDate hire = effectiveHireDate(user);
        LocalDate effectiveStart = hire.isAfter(jan1) ? hire : jan1;
        LocalDate capped = asOf.isAfter(LocalDate.of(year, 12, 31))
                ? LocalDate.of(year, 12, 31)
                : asOf;

        if (capped.isBefore(effectiveStart)) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        if (mode == FranceRttAccrualMode.MONTHLY) {
            int n = Math.max(1, monthsEmployedInCalendarYear(user, year));
            int m = monthsElapsedEmployedInYear(user, year, capped);
            if (m <= 0) {
                return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
            }
            return proratedRowTotal
                    .multiply(bd(m))
                    .divide(bd(n), SCALE, RoundingMode.HALF_UP);
        }
        return proratedRowTotal;
    }

    public static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal maxZero(BigDecimal v) {
        if (v.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }
}

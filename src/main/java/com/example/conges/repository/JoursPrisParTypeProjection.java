package com.example.conges.repository;

import com.example.conges.entity.TypeConge;
import java.math.BigDecimal;

/**
 * Projection pour la somme des {@code nombreJours} groupée par {@link TypeConge}.
 */
public interface JoursPrisParTypeProjection {

    TypeConge getTypeConge();

    BigDecimal getTotalJours();
}

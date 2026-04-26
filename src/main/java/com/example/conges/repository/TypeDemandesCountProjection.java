package com.example.conges.repository;

import com.example.conges.entity.TypeConge;

public interface TypeDemandesCountProjection {

    TypeConge getTypeConge();

    Long getTotal();
}

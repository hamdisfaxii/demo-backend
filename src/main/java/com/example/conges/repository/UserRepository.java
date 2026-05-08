package com.example.conges.repository;

import com.example.conges.entity.Role;
import com.example.conges.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByDolibarrId(Long dolibarrId);

    boolean existsByEmail(String email);

    List<UserEntity> findByRole(Role role);

    @Query("""
            SELECT u
            FROM UserEntity u
            WHERE (:q IS NULL
               OR LOWER(CONCAT(COALESCE(u.prenom,''), ' ', COALESCE(u.nom,''), ' ', COALESCE(u.email,''))) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:country IS NULL OR UPPER(COALESCE(u.pays,'')) = UPPER(:country))
              AND (:role IS NULL OR u.role = :role)
            ORDER BY u.prenom ASC, u.nom ASC, u.email ASC
            """)
    Page<UserEntity> searchUsers(
            @Param("q") String q,
            @Param("country") String country,
            @Param("role") Role role,
            Pageable pageable);
}

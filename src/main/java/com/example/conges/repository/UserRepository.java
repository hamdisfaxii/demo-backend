package com.example.conges.repository;

import com.example.conges.entity.Role;
import com.example.conges.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByDolibarrId(Long dolibarrId);

    boolean existsByEmail(String email);

    List<UserEntity> findByRole(Role role);
}

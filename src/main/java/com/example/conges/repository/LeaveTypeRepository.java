package com.example.conges.repository;

import com.example.conges.entity.LeaveType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    Optional<LeaveType> findByDolibarrLeaveTypeId(Long dolibarrLeaveTypeId);

    Optional<LeaveType> findByCode(String code);

    List<LeaveType> findByActiveTrue();
}

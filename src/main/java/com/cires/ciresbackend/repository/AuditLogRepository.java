package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Change findByActionType to findByAction
    List<AuditLog> findByAction(String action);
}
package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    // JpaRepository already includes save(), findById(), and findAll()
}

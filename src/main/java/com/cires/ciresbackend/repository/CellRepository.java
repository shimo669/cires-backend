package com.cires.ciresbackend.repository;
import com.cires.ciresbackend.entity.Cell;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CellRepository extends JpaRepository<Cell, Long> {
    List<Cell> findBySectorId(Long sectorId);
}
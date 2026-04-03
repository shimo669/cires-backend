package com.cires.ciresbackend.repository;
import com.cires.ciresbackend.entity.Village;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VillageRepository extends JpaRepository<Village, Long> {
    List<Village> findByCellId(Long cellId);
}
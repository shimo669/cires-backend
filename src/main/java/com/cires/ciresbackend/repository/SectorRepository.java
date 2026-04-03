package com.cires.ciresbackend.repository;
import com.cires.ciresbackend.entity.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SectorRepository extends JpaRepository<Sector, Long> {
    List<Sector> findByDistrictId(Long districtId);
}
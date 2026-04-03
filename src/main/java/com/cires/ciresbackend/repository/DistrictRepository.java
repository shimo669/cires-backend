package com.cires.ciresbackend.repository;
import com.cires.ciresbackend.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByProvinceId(Long provinceId);
}
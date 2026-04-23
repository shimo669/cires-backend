package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.UserResponseDTO;
import com.cires.ciresbackend.entity.User;
import com.cires.ciresbackend.repository.CategoryRepository;
import com.cires.ciresbackend.repository.CellRepository;
import com.cires.ciresbackend.repository.DistrictRepository;
import com.cires.ciresbackend.repository.FeedbackRepository;
import com.cires.ciresbackend.repository.ProvinceRepository;
import com.cires.ciresbackend.repository.ReportRepository;
import com.cires.ciresbackend.repository.RoleRepository;
import com.cires.ciresbackend.repository.SectorRepository;
import com.cires.ciresbackend.repository.SlaConfigRepository;
import com.cires.ciresbackend.repository.SlaTimerRepository;
import com.cires.ciresbackend.repository.UserRepository;
import com.cires.ciresbackend.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceUserMaskingTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private SlaConfigRepository slaConfigRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProvinceRepository provinceRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private CellRepository cellRepository;
    @Mock
    private VillageRepository villageRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private SlaTimerRepository slaTimerRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                roleRepository,
                slaConfigRepository,
                categoryRepository,
                provinceRepository,
                districtRepository,
                sectorRepository,
                cellRepository,
                villageRepository,
                reportRepository,
                feedbackRepository,
                slaTimerRepository
        );
    }

    @Test
    void getAllUsers_masksNationalIdAndShowsLastThreeDigits() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin-view-user");
        user.setEmail("user@example.com");
        user.setNationalId("1199701234567890");

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDTO> users = adminService.getAllUsers();

        assertEquals(1, users.size());
        assertEquals("*************890", users.get(0).getNationalId());
    }
}


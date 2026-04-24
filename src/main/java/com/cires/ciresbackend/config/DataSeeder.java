package com.cires.ciresbackend.config;

import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            ProvinceRepository provinceRepo,
            DistrictRepository districtRepo,
            SectorRepository sectorRepo,
            CellRepository cellRepo,
            VillageRepository villageRepo,
            CategoryRepository categoryRepo,
            RoleRepository roleRepo,
            UserRepository userRepo,
            SlaConfigRepository slaConfigRepo,
            ReportRepository reportRepo,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {

            // --- PHASE 1: SEED CATEGORIES ---
            if (categoryRepo.count() == 0) {
                System.out.println("Seeding default categories...");
                List<String> defaultCategories = List.of("Water", "Electricity", "Roads", "Security", "Health", "Other");

                for (String name : defaultCategories) {
                    Category category = new Category();
                    category.setCategoryName(name);
                    category.setDescription("Issues related to " + name);
                    categoryRepo.save(category);
                }
                System.out.println("Default categories successfully seeded!");
            }

            // --- PHASE 1.5: SEED ROLES & ADMIN USER ---
            if (roleRepo.count() == 0) {
                System.out.println("Seeding default roles...");
                Role adminRole = new Role(null, "ADMIN");
                Role leaderRole = new Role(null, "LEADER");
                Role citizenRole = new Role(null, "CITIZEN");
                roleRepo.saveAll(List.of(adminRole, leaderRole, citizenRole));
                System.out.println("Default roles successfully seeded!");
            }

            if (userRepo.count() == 0) {
                System.out.println("Seeding default admin user...");
                Role adminRole = roleRepo.findByRoleName("ADMIN")
                        .orElseThrow(() -> new RuntimeException("Admin role not found"));

                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@cires.com");
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setNationalId("1234567890123456");
                admin.setRole(adminRole);
                admin.setLevelType(User.UserLevelType.NATIONAL_ADMIN);
                
                userRepo.save(admin);
                System.out.println("Default admin user successfully seeded! (admin / Admin123!)");
            }

            // --- PHASE 1.7: SEED SLA CONFIGURATIONS ---
            if (slaConfigRepo.count() == 0) {
                System.out.println("Seeding default SLA configurations...");
                List<Category> categories = categoryRepo.findAll();
                GovernmentLevelType[] levels = GovernmentLevelType.values();

                for (Category cat : categories) {
                    for (GovernmentLevelType level : levels) {
                        SlaConfig config = new SlaConfig();
                        config.setCategory(cat);
                        config.setLevelType(level);
                        
                        // Special case for Health category: 20 minutes
                        if (cat.getCategoryName().equalsIgnoreCase("Health") || cat.getCategoryName().equalsIgnoreCase("Santé")) {
                            config.setDurationMinutes(20);
                        } else {
                            // Default durations converted to minutes: Village: 24h, Cell: 48h, etc.
                            int minutes = switch (level) {
                                case VILLAGE -> 24 * 60;
                                case CELL -> 48 * 60;
                                case SECTOR -> 72 * 60;
                                case DISTRICT -> 120 * 60;
                                case PROVINCE -> 168 * 60;
                                case NATIONAL -> 240 * 60;
                            };
                            config.setDurationMinutes(minutes);
                        }
                        slaConfigRepo.save(config);
                    }
                }
                System.out.println("Default SLA configurations successfully seeded!");
            }

            // --- PHASE 1.8: SEED SAMPLE REPORTS ---
            if (reportRepo.count() == 0) {
                System.out.println("Seeding sample reports...");
                User admin = userRepo.findByUsername("admin").orElse(null);
                List<Category> categories = categoryRepo.findAll();
                List<Village> villages = villageRepo.findAll();

                if (admin != null && !categories.isEmpty() && !villages.isEmpty()) {
                    for (int i = 1; i <= 5; i++) {
                        Report report = new Report();
                        report.setTitle("Sample Issue #" + i);
                        report.setDescription("This is a sample report description for issue " + i);
                        report.setReporter(admin);
                        Category category = categories.get(i % categories.size());
                        report.setCategory(category);
                        report.setIncidentVillage(villages.get(i % villages.size()));
                        report.setStatus("PENDING");
                        report.setCurrentEscalationLevel(Report.EscalationLevel.AT_VILLAGE);

                        // If it's a Health issue, use the 20-minute rule
                        boolean isHealth = category.getCategoryName().equalsIgnoreCase("Health") || category.getCategoryName().equalsIgnoreCase("Santé");

                        if (i <= 2) {
                            // Overdue: 2 days ago (or 30 mins ago if Health)
                            int overdueMinutes = isHealth ? 30 : 2880; // 2880 = 2 days
                            report.setSlaDeadline(java.time.LocalDateTime.now().minusMinutes(overdueMinutes));
                        } else {
                            // Active: 2 days from now (or 10 mins from now if Health)
                            int activeMinutes = isHealth ? 10 : 2880;
                            report.setSlaDeadline(java.time.LocalDateTime.now().plusMinutes(activeMinutes));
                        }

                        reportRepo.save(report);
                    }
                    System.out.println("5 sample reports successfully seeded!");
                }
            }

            // --- PHASE 2: SEED GEOGRAPHY ---
            if (villageRepo.count() > 0) {
                System.out.println("Geography already seeded, skipping locations.json import.");
                return;
            }

            System.out.println("Seeding geographic data from locations.json...");
            ObjectMapper mapper = new ObjectMapper();

            TypeReference<List<Map<String, Object>>> typeReference = new TypeReference<>() {};
            try (InputStream inputStream = DataSeeder.class.getResourceAsStream("/locations.json")) {
                if (inputStream == null) {
                    System.out.println("Error: locations.json not found in resources folder.");
                    return;
                }

                List<Map<String, Object>> locations = mapper.readValue(inputStream, typeReference);

                Map<String, Province> provinceMap = provinceRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                Province::getName,
                                province -> province,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, District> districtMap = districtRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                district -> key(district.getProvince().getName(), district.getName()),
                                district -> district,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, Sector> sectorMap = sectorRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                sector -> key(sector.getDistrict().getProvince().getName(), sector.getDistrict().getName(), sector.getName()),
                                sector -> sector,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, Cell> cellMap = cellRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                cell -> key(cell.getSector().getDistrict().getProvince().getName(), cell.getSector().getDistrict().getName(), cell.getSector().getName(), cell.getName()),
                                cell -> cell,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, Village> villageMap = villageRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                village -> key(village.getCell().getSector().getDistrict().getProvince().getName(), village.getCell().getSector().getDistrict().getName(), village.getCell().getSector().getName(), village.getCell().getName(), village.getName()),
                                village -> village,
                                (existing, duplicate) -> existing,
                                HashMap::new));

                for (Map<String, Object> loc : locations) {
                    String provName = stringValue(loc, "province_name");
                    String distName = stringValue(loc, "district_name");
                    String sectName = stringValue(loc, "sector_name");
                    String cellName = stringValue(loc, "cell_name");
                    String villName = stringValue(loc, "village_name");

                    if (provName == null || distName == null || sectName == null || cellName == null || villName == null) {
                        continue;
                    }

                    Province province = provinceMap.computeIfAbsent(provName, name -> provinceRepo.save(new Province(null, name)));
                    String districtKey = key(provName, distName);
                    District district = districtMap.computeIfAbsent(districtKey, k -> districtRepo.save(new District(null, distName, province)));

                    String sectorKey = key(provName, distName, sectName);
                    Sector sector = sectorMap.computeIfAbsent(sectorKey, k -> sectorRepo.save(new Sector(null, sectName, district)));

                    String cellKey = key(provName, distName, sectName, cellName);
                    Cell cell = cellMap.computeIfAbsent(cellKey, k -> cellRepo.save(new Cell(null, cellName, sector)));

                    String villageKey = key(provName, distName, sectName, cellName, villName);
                    villageMap.computeIfAbsent(villageKey, k -> villageRepo.save(new Village(null, villName, cell)));
                }

                System.out.println("Geography data successfully seeded!");
            } catch (Exception e) {
                System.out.println("Unable to save location data: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private String key(String... parts) {
        return String.join("|", Arrays.stream(parts)
                .map(this::normalize)
                .toArray(String[]::new));
    }

    private String stringValue(Map<String, Object> location, String key) {
        Object value = location.get(key);
        return value == null ? null : value.toString().trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
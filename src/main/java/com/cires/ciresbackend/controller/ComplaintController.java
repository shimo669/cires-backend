package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.entity.Complaint;
import com.cires.ciresbackend.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@CrossOrigin("*") // Allows your frontend to connect without security blocks
public class ComplaintController {

    private final ComplaintService service;

    @GetMapping
    public List<Complaint> getAll() {
        return service.getAllComplaints();
    }

    @PostMapping
    public Complaint create(@RequestBody Complaint complaint) {
        return service.saveComplaint(complaint);
    }

    @GetMapping("/{id}")
    public Complaint getById(@PathVariable Long id) {
        return service.getComplaintById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteComplaint(id);
    }
}
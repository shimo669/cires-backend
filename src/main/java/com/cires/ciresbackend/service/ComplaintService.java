package com.cires.ciresbackend.service;

import com.cires.ciresbackend.entity.Complaint;
import com.cires.ciresbackend.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository repository;

    public List<Complaint> getAllComplaints() {
        return repository.findAll();
    }

    public Complaint saveComplaint(Complaint complaint) {
        // You can set a default status here if it's new
        if (complaint.getStatus() == null) {
            complaint.setStatus("PENDING");
        }
        return repository.save(complaint);
    }

    public Complaint getComplaintById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));
    }

    public void deleteComplaint(Long id) {
        repository.deleteById(id);
    }
}
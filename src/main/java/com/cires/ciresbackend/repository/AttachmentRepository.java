package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByReportId(Long reportId);
    List<Attachment> findByFileType(String fileType);
}


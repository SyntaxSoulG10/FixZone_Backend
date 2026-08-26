package com.fixzone.fixzon_backend.repository;

import com.fixzone.fixzon_backend.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByOwnerCode(String ownerCode);
}

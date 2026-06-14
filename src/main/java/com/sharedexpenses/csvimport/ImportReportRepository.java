package com.sharedexpenses.csvimport;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ImportReportRepository extends JpaRepository<ImportReport, Long> {

    List<ImportReport> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Optional<ImportReport> findByIdAndGroupId(Long id, Long groupId);
}

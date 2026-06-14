package com.sharedexpenses.csvimport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Optional<ImportJob> findByIdAndGroupId(Long id, Long groupId);
}

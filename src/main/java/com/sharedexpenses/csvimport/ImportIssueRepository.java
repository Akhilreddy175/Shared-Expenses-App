package com.sharedexpenses.csvimport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportIssueRepository extends JpaRepository<ImportIssue, Long> {

    List<ImportIssue> findByImportRowIdIn(List<Long> importRowIds);
}

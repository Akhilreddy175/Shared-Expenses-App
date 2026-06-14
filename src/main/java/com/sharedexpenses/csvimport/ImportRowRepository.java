package com.sharedexpenses.csvimport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportRowRepository extends JpaRepository<ImportRow, Long> {

    List<ImportRow> findByImportJobIdOrderByRowNumber(Long importJobId);

    List<ImportRow> findByImportJobIdAndStatusOrderByRowNumber(Long importJobId, ImportRowStatus status);
}

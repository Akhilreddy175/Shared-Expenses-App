package com.sharedexpenses.csvimport;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportReviewRepository extends JpaRepository<ImportReview, Long> {

    Optional<ImportReview> findByImportJobId(Long importJobId);

    boolean existsByImportJobId(Long importJobId);
}

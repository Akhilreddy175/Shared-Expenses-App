package com.sharedexpenses.csvimport;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    List<Anomaly> findByImportRowIdIn(List<Long> importRowIds);
}

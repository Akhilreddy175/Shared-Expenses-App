package com.sharedexpenses.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // For listing settlements in a group, newest first
    List<Settlement> findByGroupIdOrderBySettlementDateDesc(Long groupId);

    // For balance computation — order doesn't matter when summing
    List<Settlement> findByGroupId(Long groupId);

    // Ensures the settlement belongs to the stated group before returning it
    Optional<Settlement> findByIdAndGroupId(Long id, Long groupId);
}

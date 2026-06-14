package com.sharedexpenses.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    
    List<Settlement> findByGroupIdOrderBySettlementDateDesc(Long groupId);

    
    List<Settlement> findByGroupId(Long groupId);

    
    Optional<Settlement> findByIdAndGroupId(Long id, Long groupId);
}

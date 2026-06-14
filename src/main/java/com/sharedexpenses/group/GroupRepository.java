package com.sharedexpenses.group;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    // JpaRepository gives us findById, save, delete, findAll.
    // No custom queries needed here — group lookups are all by primary key.
}

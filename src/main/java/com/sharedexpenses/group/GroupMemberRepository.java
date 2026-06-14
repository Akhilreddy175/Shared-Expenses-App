package com.sharedexpenses.group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // Active members of a group (left_at IS NULL)
    List<GroupMember> findByGroupIdAndLeftAtIsNull(Long groupId);

    // Full history — everyone who was ever in this group
    List<GroupMember> findByGroupId(Long groupId);

    // Check if a user is currently active in a group (used for authorization and duplicate checks)
    Optional<GroupMember> findByGroupIdAndUserIdAndLeftAtIsNull(Long groupId, Long userId);

    boolean existsByGroupIdAndUserIdAndLeftAtIsNull(Long groupId, Long userId);

    // All groups the user is currently active in (for "my groups" list)
    List<GroupMember> findByUserIdAndLeftAtIsNull(Long userId);

    // Was this user active on a specific date?
    // Used in Phase 4+ to validate expense participants against membership dates.
    @Query("SELECT m FROM GroupMember m WHERE m.groupId = :groupId AND m.userId = :userId " +
           "AND m.joinedAt <= :date AND (m.leftAt IS NULL OR m.leftAt > :date)")
    Optional<GroupMember> findActiveMemberOnDate(@Param("groupId") Long groupId,
                                                 @Param("userId") Long userId,
                                                 @Param("date") LocalDate date);
}

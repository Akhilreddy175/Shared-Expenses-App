package com.sharedexpenses.group;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupIdAndLeftAtIsNull(Long groupId);

    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndUserIdAndLeftAtIsNull(Long groupId, Long userId);

    boolean existsByGroupIdAndUserIdAndLeftAtIsNull(Long groupId, Long userId);

    List<GroupMember> findByUserIdAndLeftAtIsNull(Long userId);

    @Query("SELECT m FROM GroupMember m WHERE m.groupId = :groupId AND m.userId = :userId " +
           "AND m.joinedAt <= :date AND (m.leftAt IS NULL OR m.leftAt > :date)")
    Optional<GroupMember> findActiveMemberOnDate(@Param("groupId") Long groupId,
                                                 @Param("userId") Long userId,
                                                 @Param("date") LocalDate date);
}

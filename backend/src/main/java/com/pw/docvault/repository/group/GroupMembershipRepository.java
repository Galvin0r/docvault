package com.pw.docvault.repository.group;

import com.pw.docvault.entity.group.GroupMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    Optional<GroupMembership> findByUserIdAndGroupId(Long userId, Long groupId);
    long countAllByGroupId(Long groupId);
    Page<GroupMembership> findAllByGroupId(Long groupId, Pageable pageable);
    List<GroupMembership> findAllByUserId(Long userId);
    @EntityGraph(attributePaths = {"group"})
    @Query("""
        select gm
        from GroupMembership gm
        join gm.group g
        where gm.user.id = :userId
          and (
                :groupName is null
                or :groupName = ''
                or lower(g.name) like lower(concat('%', :groupName, '%'))
          )
          and (
                :viewerId = :userId
                or g.visibility in (
                    com.pw.docvault.model.enums.GroupVisibility.PUBLIC,
                    com.pw.docvault.model.enums.GroupVisibility.REQUEST_ONLY
                )
                or exists (
                    select 1
                    from GroupMembership vgm
                    where vgm.user.id = :viewerId
                      and vgm.group.id = g.id
                )
          )
        """)
    Page<GroupMembership> findMembershipsVisibleToViewer(@Param("userId") Long userId,
                                                         @Param("viewerId") Long viewerId,
                                                         @Param("groupName") String groupName,
                                                         Pageable pageable);
}
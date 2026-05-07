package com.pw.docvault.repository.group;


import com.pw.docvault.entity.group.Group;
import com.pw.docvault.model.enums.GroupVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("""
        select g
        from Group g
        where lower(g.name) like lower(concat('%', :name, '%'))
          and (
            g.visibility <> com.pw.docvault.model.enums.GroupVisibility.PRIVATE
            or exists (
              select 1
              from GroupMembership gm
              where gm.group.id = g.id
                and gm.user.id = :userId
            )
          )
        """)
    Page<Group> searchVisibleToUser(@Param("name") String name,
                                    @Param("userId") Long userId,
                                    Pageable pageable);
}
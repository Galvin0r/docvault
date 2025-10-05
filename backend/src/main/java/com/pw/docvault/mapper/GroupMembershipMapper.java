package com.pw.docvault.mapper;

import com.pw.docvault.entity.group.GroupMembership;
import com.pw.docvault.model.GroupMembershipDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMembershipMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.login", target = "userLogin")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    GroupMembershipDto toDto(GroupMembership entity);

    @InheritInverseConfiguration
    GroupMembership toEntity(GroupMembershipDto dto);
}

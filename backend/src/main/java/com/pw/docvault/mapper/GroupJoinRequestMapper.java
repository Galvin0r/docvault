package com.pw.docvault.mapper;

import com.pw.docvault.entity.group.GroupJoinRequest;
import com.pw.docvault.model.group.GroupJoinRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupJoinRequestMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.login", target = "userLogin")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    GroupJoinRequestDto toDto(GroupJoinRequest entity);

    GroupJoinRequest toEntity(GroupJoinRequestDto dto);
}
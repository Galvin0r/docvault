package com.pw.docvault.mapper;

import com.pw.docvault.entity.Group;
import com.pw.docvault.model.GroupDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    GroupDto toDto(Group group, Long membersNumber);

    @Mapping(target = "description", ignore = true)
    @Mapping(target = "created", ignore = true)
    GroupDto toSimpleDto(Group group, Long membersNumber);

    Group toEntity(GroupDto group);
}

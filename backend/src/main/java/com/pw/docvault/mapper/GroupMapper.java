package com.pw.docvault.mapper;

import com.pw.docvault.entity.Group;
import com.pw.docvault.model.GroupDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    GroupDto toDto(Group group);

    Group toEntity(GroupDto group);
}

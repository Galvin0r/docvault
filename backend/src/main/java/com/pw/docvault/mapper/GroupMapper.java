package com.pw.docvault.mapper;

import com.pw.docvault.entity.Group;
import com.pw.docvault.model.GroupDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupMapper {

    public GroupDto toDto(Group group);

    public Group toEntity(GroupDto group);
}

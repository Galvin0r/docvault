package com.pw.docvault.mapper;

import com.pw.docvault.entity.GroupJoinRequest;
import com.pw.docvault.model.GroupJoinRequestDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GroupJoinRequestMapper {

    GroupJoinRequestDto toDto(GroupJoinRequest entity);

    GroupJoinRequest toEntity(GroupJoinRequestDto dto);
}

package com.pw.docvault.mapper;

import com.pw.docvault.entity.document.DocumentAccess;
import com.pw.docvault.model.document.DocumentAccessDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentAccessMapper {
    @Mapping(source = "document.id", target = "documentId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.login", target = "userLogin")
    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "group.name", target = "groupName")
    DocumentAccessDto toDto(DocumentAccess entity);

    @InheritInverseConfiguration
    @Mapping(target = "permission", ignore = true)
    DocumentAccess toEntity(DocumentAccessDto dto);
}

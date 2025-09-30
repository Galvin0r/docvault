package com.pw.docvault.mapper;

import com.pw.docvault.entity.Document;
import com.pw.docvault.model.DocumentDto;
import com.pw.docvault.model.enums.DocumentVisibility;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "owner.login", target = "ownerLogin")
    DocumentDto toDto(Document entity);

    @InheritInverseConfiguration
    Document toEntity(DocumentDto dto);
}

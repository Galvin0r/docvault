package com.pw.docvault.mapper;

import com.pw.docvault.entity.document.Document;
import com.pw.docvault.model.document.DocumentDto;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "owner.login", target = "ownerLogin")
    @Mapping(source = "sizeBytes", target = "size")
    @Mapping(source = "created", target = "uploadedAt")
    @Mapping(target = "attempts", ignore = true)
    @Mapping(target = "nextAttemptAt", ignore = true)
    DocumentDto toDto(Document entity);

    @InheritInverseConfiguration
    Document toEntity(DocumentDto dto);
}
package com.pw.docvault.mapper;

import com.pw.docvault.entity.document.DocumentFragment;
import com.pw.docvault.model.document.DocumentFragmentDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentFragmentMapper {

    DocumentFragmentDTO toDTO(DocumentFragment documentFragment);
}
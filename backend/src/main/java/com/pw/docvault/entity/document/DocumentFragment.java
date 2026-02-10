package com.pw.docvault.entity.document;

import com.pw.docvault.model.enums.DocumentVisibility;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document(indexName = "doc_fragment")
@Setting(settingPath = "/elasticsearch/doc_fragments-settings.json")
public class DocumentFragment {

    private static final int EMBEDDING_DIMS = 384;

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long documentId;

    @Field(type = FieldType.Integer)
    private Integer fragmentOrder;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Keyword)
    private DocumentVisibility visibility;

    @Field(type = FieldType.Long)
    private List<Long> permittedUserIds;

    @Field(type = FieldType.Long)
    private List<Long> permittedGroupIds;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Dense_Vector, dims = EMBEDDING_DIMS)
    private float[] embedding;
}

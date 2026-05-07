from pydantic import BaseModel, Field, HttpUrl


class ProcessRequest(BaseModel):
    signedUrl: HttpUrl
    mimeType: str = Field(min_length=1)


class EmbedRequest(BaseModel):
    text: str = Field(min_length=1)


class EmbedResponse(BaseModel):
    embedding: list[float]


class ProcessedFragment(BaseModel):
    fragmentOrder: int
    content: str
    embedding: list[float]


class HealthResponse(BaseModel):
    status: str
    modelName: str
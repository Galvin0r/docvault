from dataclasses import dataclass
from functools import lru_cache
import os


@dataclass(frozen=True)
class Settings:
    model_name: str
    device: str
    chunk_size_chars: int
    chunk_overlap_chars: int
    embedding_batch_size: int
    request_timeout_seconds: int


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings(
        model_name=os.getenv("PROCESSOR_MODEL_NAME", "sentence-transformers/all-MiniLM-L6-v2"),
        device=os.getenv("PROCESSOR_DEVICE", "cpu"),
        chunk_size_chars=int(os.getenv("PROCESSOR_CHUNK_SIZE_CHARS", "1200")),
        chunk_overlap_chars=int(os.getenv("PROCESSOR_CHUNK_OVERLAP_CHARS", "150")),
        embedding_batch_size=int(os.getenv("PROCESSOR_EMBEDDING_BATCH_SIZE", "16")),
        request_timeout_seconds=int(os.getenv("PROCESSOR_REQUEST_TIMEOUT_SECONDS", "180")),
    )

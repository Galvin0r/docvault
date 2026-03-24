from functools import lru_cache

from sentence_transformers import SentenceTransformer

from .config import get_settings


@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    settings = get_settings()
    return SentenceTransformer(settings.model_name, device=settings.device)


def embed_texts(texts: list[str]) -> list[list[float]]:
    settings = get_settings()
    model = get_model()
    vectors = model.encode(
        texts,
        batch_size=settings.embedding_batch_size,
        convert_to_numpy=True,
        normalize_embeddings=True,
        show_progress_bar=False,
    )
    return [[float(value) for value in vector.tolist()] for vector in vectors]

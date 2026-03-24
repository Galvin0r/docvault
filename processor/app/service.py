import json
import logging
import tempfile
from pathlib import Path
from typing import Iterator

import requests

from .chunking import iter_text_chunks
from .config import get_settings
from .embedding import embed_texts
from .extraction import extract_text
from .schemas import ProcessedFragment

logger = logging.getLogger(__name__)

_DOWNLOAD_CHUNK_SIZE = 8192


def download_to_file(signed_url: str, dest_path: Path) -> None:
    settings = get_settings()
    with requests.get(signed_url, stream=True, timeout=(10, settings.request_timeout_seconds)) as response:
        response.raise_for_status()
        with open(dest_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=_DOWNLOAD_CHUNK_SIZE):
                f.write(chunk)
    logger.info("Downloaded document to temp file (%d bytes)", dest_path.stat().st_size)


def extract_document_text(signed_url: str, mime_type: str) -> str:
    with tempfile.NamedTemporaryFile(suffix=_suffix_for(mime_type)) as tmp:
        tmp_path = Path(tmp.name)
        download_to_file(signed_url, tmp_path)
        return extract_text(tmp_path, mime_type)


def stream_processed_fragments(text: str) -> Iterator[str]:
    settings = get_settings()
    pending_batch: list[tuple[int, str]] = []

    for fragment_order, chunk in enumerate(
            iter_text_chunks(text, settings.chunk_size_chars, settings.chunk_overlap_chars)
    ):
        pending_batch.append((fragment_order, chunk))
        if len(pending_batch) >= settings.embedding_batch_size:
            yield from _serialize_batch(pending_batch)
            pending_batch.clear()

    if pending_batch:
        yield from _serialize_batch(pending_batch)


def _serialize_batch(batch: list[tuple[int, str]]) -> Iterator[str]:
    embeddings = embed_texts([chunk for _, chunk in batch])
    for (fragment_order, chunk), embedding in zip(batch, embeddings, strict=True):
        fragment = ProcessedFragment(
            fragmentOrder=fragment_order,
            content=chunk,
            embedding=embedding,
        )
        yield json.dumps(fragment.model_dump(), separators=(",", ":")) + "\n"


def _suffix_for(mime_type: str) -> str:
    normalized = mime_type.split(";", 1)[0].strip().lower()
    suffixes = {
        "application/pdf": ".pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ".docx",
        "text/plain": ".txt",
        "application/epub+zip": ".epub",
    }
    return suffixes.get(normalized, "")

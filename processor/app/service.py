import json
import logging
import tempfile
from pathlib import Path
from typing import Iterator
from itertools import chain

import requests

from .chunking import iter_text_chunks_from_units
from .config import get_settings
from .embedding import embed_texts
from .extraction import iter_extracted_text_units
from .extractors import suffix_for_mime_type
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


def stream_processed_fragments_from_document(signed_url: str, mime_type: str) -> Iterator[str]:
    with tempfile.NamedTemporaryFile(suffix=_suffix_for(mime_type)) as tmp:
        tmp_path = Path(tmp.name)
        download_to_file(signed_url, tmp_path)

        units = iter_extracted_text_units(tmp_path, mime_type)
        first_unit = next(units)
        yield from stream_processed_fragments_from_units(chain([first_unit], units))


def stream_processed_fragments(text: str) -> Iterator[str]:
    yield from stream_processed_fragments_from_units(iter([text]))


def stream_processed_fragments_from_units(units: Iterator[str]) -> Iterator[str]:
    settings = get_settings()
    pending_batch: list[tuple[int, str]] = []

    chunk_iterator = iter_text_chunks_from_units(
        units,
        settings.chunk_size_chars,
        settings.chunk_overlap_chars,
        settings.min_chunk_size_chars,
    )

    for fragment_order, chunk in enumerate(
            chunk_iterator
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
    return suffix_for_mime_type(mime_type)
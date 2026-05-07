from collections.abc import Iterator
import logging
from pathlib import Path

from .chunking import normalize_text
from .extractors import get_extractor, normalize_mime_type

logger = logging.getLogger(__name__)


class UnsupportedMimeTypeError(ValueError):
    pass


class EmptyDocumentError(ValueError):
    pass


def iter_extracted_text_units(file_path: Path, mime_type: str) -> Iterator[str]:
    normalized_mime_type = normalize_mime_type(mime_type)
    extractor = get_extractor(normalized_mime_type)
    if extractor is None:
        raise UnsupportedMimeTypeError(f"Unsupported mime type: {mime_type}")

    yielded = 0
    total_characters = 0

    for unit in extractor.iter_text_units(file_path):
        normalized_unit = normalize_text(unit)
        if not normalized_unit:
            continue
        yielded += 1
        total_characters += len(normalized_unit)
        yield normalized_unit

    if yielded == 0:
        raise EmptyDocumentError("Document did not contain any extractable text")

    logger.info("Extracted %d characters from %s document", total_characters, normalized_mime_type)